# [2/6] store 도메인 개발

제휴 쿠폰 서비스 백엔드의 store 도메인을 개발한다. auth 도메인(회원/JWT/공통 에러 인프라)은 이미 구현되어 있다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 이 문서에 적힌 범위까지만 개발한다. 다음 도메인(partnership)을 미리 시작하지 마라.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `feat: 가게 등록 API 구현`, `feat: 메뉴 일괄 수정 API 구현`
- 권장 커밋 단위: STORES 엔티티+리포지토리 → MENUS 엔티티+리포지토리 → 거리 계산 유틸 → 기능별 (서비스+컨트롤러+테스트를 기능 단위로)

## 기존 공통 규칙 (이미 구현됨 — 그대로 따를 것)

- 에러 응답: `application/problem+json` + `code` 필드. 기존 `ErrorCode` 인터페이스/전역 핸들러 재사용
- 인증: `Authorization: Bearer {accessToken}`, role 인가(OWNER/CUSTOMER). 역할 위반은 `USR_403`, 입력값 오류는 `GLB_400`

### 이번 단계에서 추가할 에러 코드 (StoreErrorCode)

| code | enum | HTTP | message |
|---|---|---|---|
| STR_403 | NOT_STORE_OWNER | 403 | 본인 소유의 가게가 아닙니다 |
| STR_404 | STORE_NOT_FOUND | 404 | 존재하지 않는 가게입니다 |
| STR_409 | STORE_ALREADY_EXISTS | 409 | 이미 등록된 가게가 있습니다 |

## DB 설계

### STORES

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| owner_user_id | BIGINT FK → USERS | 1인 1가게 |
| name | VARCHAR(100) | |
| category | VARCHAR(30) | KOREAN \| CHINESE \| JAPANESE \| WESTERN \| SNACK \| CAFE |
| address | VARCHAR(255) | |
| phone | VARCHAR(20) | |
| avg_price | INT | 1인 평균 가격, 추천 필터 기준 |
| description | VARCHAR(500) | |
| latitude | DOUBLE PRECISION | 지도 표시용, `(latitude, longitude)` 복합 인덱스 |
| longitude | DOUBLE PRECISION | |
| open_time | TIME | 영업 시작 (HH:mm) |
| close_time | TIME | 영업 종료 (HH:mm) |
| min_order_amount | INT | 최소 주문 금액 |
| created_at | DATETIME | |

### MENUS

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| store_id | BIGINT FK → STORES | |
| name | VARCHAR(100) | |
| description | VARCHAR(500) | |
| price | INT | 정가 |
| discounted_price | INT NULL | 할인가. 없으면 NULL, 있으면 price보다 작아야 함 |
| display_order | INT | 표시 순서 |
| created_at | DATETIME | |

## 거리 계산 (공통 규칙)

- 조회 API(상세/지도/추천)에 사용자 좌표 `lat, lng`가 오면 각 가게에 `distanceMeters`(하버사인 공식), `walkingMinutes`(도보 80m/분, 올림)를 포함한다. 좌표가 없으면 둘 다 null.
- 프론트가 "도보 4분, 0.3km" 형태로 포맷팅한다. 서버는 숫자만 내려준다.
- 공용 유틸로 구현해 store/recommendation 도메인에서 재사용한다.

## API 명세 (9개)

### 1. 가게 등록 — `POST /stores` (OWNER)

사장님이 자신의 가게를 등록한다. 카테고리·1인 평균 가격 필수(추천 필터 기준), 위도/경도 필수(지도 표시, 지오코딩은 프론트 처리). 영업시간·최소 주문 금액·메뉴 목록을 함께 등록한다.

Request: `Authorization: Bearer {accessToken}` (OWNER)
```json
{
  "name": "시흔식당",
  "category": "KOREAN",
  "address": "대전시 유성구 ...",
  "latitude": 36.3624,
  "longitude": 127.3568,
  "phone": "042-000-0000",
  "avgPrice": 9000,
  "description": "백반 전문점",
  "openTime": "09:00",
  "closeTime": "21:00",
  "minOrderAmount": 8000,
  "menus": [
    { "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 }
  ]
}
```
- menus: 각 항목 name, description, price, discountedPrice(선택 — 없으면 null)

Response 201:
```json
{ "storeId": 10 }
```

에러: 400 `GLB_400`, 403 `USR_403` (OWNER 아님), 409 `STR_409` (이미 등록된 가게 존재)

### 2. 가게 상세 조회 — `GET /stores/{storeId}` (인증 필요)

메뉴·영업시간·최소 주문 금액·거리 포함 상세 조회. 사용자 앱에서는 사용 가능한 내 쿠폰 개수를 함께 내려준다.
(`usableCouponCount`는 coupon 도메인 완성 전까지 0으로 두고 TODO 주석을 남겨라.)

Query: `lat, lng`(선택) — 전달 시 거리 정보 포함

Response 200:
```json
{
  "storeId": 10,
  "name": "시흔식당",
  "category": "KOREAN",
  "address": "대전시 유성구 ...",
  "latitude": 36.3624,
  "longitude": 127.3568,
  "phone": "042-000-0000",
  "avgPrice": 9000,
  "description": "백반 전문점",
  "openTime": "09:00",
  "closeTime": "21:00",
  "minOrderAmount": 8000,
  "distanceMeters": 320,
  "walkingMinutes": 4,
  "menus": [
    { "menuId": 1, "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 }
  ],
  "usableCouponCount": 2
}
```

에러: 404 `STR_404`

### 3. 내 가게 조회 — `GET /stores/me` (OWNER)

로그인한 사장님의 가게 조회. 웹 대시보드 진입 시 사용.

Response 200: 가게 상세 조회와 동일한 형식 (거리 정보는 null)

에러: 404 `STR_404` (등록된 가게 없음)

### 4. 가게 정보 수정 — `PATCH /stores/{storeId}` (OWNER, 본인 가게)

부분 수정. openTime, closeTime, minOrderAmount도 수정 가능. 메뉴는 별도 API(메뉴 일괄 수정)로 처리.

Request:
```json
{ "avgPrice": 10000, "description": "백반, 찌개 전문점", "minOrderAmount": 9000 }
```

Response 200: 수정된 가게 정보 전체 반환

에러: 403 `STR_403` (본인 가게 아님), 404 `STR_404`

### 5. 메뉴 일괄 수정 — `PUT /stores/{storeId}/menus` (OWNER, 본인 가게) **[신규]**

메뉴 목록 전체 교체(replace). 요청 배열에 없는 기존 메뉴는 삭제, 배열 순서 = display_order. 멱등.

Request:
```json
{
  "menus": [
    { "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 },
    { "name": "된장찌개", "description": "집된장 사용", "price": 8000, "discountedPrice": null }
  ]
}
```

Response 200:
```json
{
  "menus": [
    { "menuId": 1, "name": "제육볶음", "description": "매콤한 제육", "price": 9000, "discountedPrice": 8000 },
    { "menuId": 2, "name": "된장찌개", "description": "집된장 사용", "price": 8000, "discountedPrice": null }
  ]
}
```

에러: 400 `GLB_400` (할인가 ≥ 정가 포함), 403 `STR_403`, 404 `STR_404`

### 6. 가게 목록 조회 — `GET /stores` (CUSTOMER) **[신규]**

사용자 앱의 목록 화면용. 카테고리·가격 필터로 가게 목록을 조회한다. 모든 필터는 선택. 세부 정보는 가게 상세 조회를 사용한다.
추천 API와의 차이: 추천은 쿠폰 사용 가능 가게 우선 노출·maxPrice 필수, 목록은 순수 필터링·전부 선택값.
(`hasUsableCoupon`은 coupon 도메인 완성 전까지 false로 두고 TODO 주석을 남겨라.)

Query:
- `category` (선택, 복수 가능), `maxPrice` (선택)
- `lat, lng` (선택): 전달 시 거리 정보 포함
- `sort` (선택): `PRICE_ASC | DISCOUNT_DESC | DISTANCE_ASC` — 기본값은 lat/lng 있으면 DISTANCE_ASC, 없으면 createdAt DESC
- `page, size`

정렬 기준:
- `PRICE_ASC`: 1인 평균 가격(avgPrice) 낮은 순
- `DISCOUNT_DESC`: 할인 많은 순 — 가게 메뉴 중 최대 할인율 `max((price - discountedPrice) / price)` 내림차순, 할인 메뉴 없는 가게는 맨 뒤. 서브쿼리/조인으로 DB에서 계산 (N+1 금지)
- `DISTANCE_ASC`: 거리 가까운 순 — lat/lng 필수, 없으면 400 `GLB_400`

Response 200:
```json
{
  "content": [
    {
      "storeId": 12,
      "name": "흔카페",
      "category": "CAFE",
      "avgPrice": 6000,
      "latitude": 36.3624,
      "longitude": 127.3568,
      "openTime": "08:00",
      "closeTime": "22:00",
      "minOrderAmount": 5000,
      "distanceMeters": 320,
      "walkingMinutes": 4,
      "hasUsableCoupon": true
    }
  ],
  "page": 0,
  "totalPages": 5
}
```

에러: 400 `GLB_400` (maxPrice 음수, 좌표 형식 오류, DISTANCE_ASC인데 lat/lng 누락 등)

라우팅 주의: `/stores/me`, `/stores/map`, `/stores/search`, `/stores/categories`가 `/stores/{storeId}`보다 먼저 매칭되도록 한다.

### 7. 지도 영역 내 가게 조회 — `GET /stores/map` (CUSTOMER)

지도 뷰포트의 바운딩 박스 내 가게 목록. 금액 제한·카테고리 필터, 쿠폰 배지, 거리·영업시간·최소 주문 금액 표시.
(`hasUsableCoupon`은 coupon 도메인 완성 전까지 false로 두고 TODO 주석을 남겨라.)

Query:
- `swLat, swLng, neLat, neLng`: 바운딩 박스 남서/북동 좌표 (필수)
- `maxPrice` (선택), `category` (선택, 복수 가능)
- `limit` (선택, 기본 100): 초과 시 truncated로 줌 확대 유도
- `lat, lng` (선택): 사용자 현재 위치 — 거리 정보 포함

Response 200:
```json
{
  "stores": [
    {
      "storeId": 12,
      "name": "흔카페",
      "category": "CAFE",
      "avgPrice": 6000,
      "latitude": 36.3624,
      "longitude": 127.3568,
      "openTime": "08:00",
      "closeTime": "22:00",
      "minOrderAmount": 5000,
      "distanceMeters": 320,
      "walkingMinutes": 4,
      "hasUsableCoupon": true
    }
  ],
  "totalInBounds": 42,
  "truncated": false
}
```

에러: 400 `GLB_400` (바운딩 박스 좌표 누락 또는 범위 오류)

비고: `(latitude, longitude)` 복합 인덱스로 조회. 가게 수 증가 시 PostGIS 공간 인덱스 검토(지금은 미적용 — 일반 컬럼으로 구현).

### 8. 음식 카테고리 목록 — `GET /stores/categories`

Response 200:
```json
{
  "categories": [
    { "code": "KOREAN", "label": "한식" },
    { "code": "CHINESE", "label": "중식" },
    { "code": "JAPANESE", "label": "일식" },
    { "code": "WESTERN", "label": "양식" },
    { "code": "SNACK", "label": "분식" },
    { "code": "CAFE", "label": "카페" }
  ]
}
```

### 9. 제휴 대상 가게 검색 — `GET /stores/search` (OWNER)

이름/카테고리로 검색, 자기 가게 제외.
(`partnershipStatus`는 partnership 도메인 완성 전까지 `NONE` 고정으로 두고 TODO 주석을 남겨라.)

Query: `keyword`, `category`(선택), `page`, `size`

Response 200:
```json
{
  "content": [
    { "storeId": 12, "name": "흔카페", "category": "CAFE", "partnershipStatus": "NONE" }
  ],
  "page": 0,
  "totalPages": 3
}
```
- partnershipStatus: `NONE | PENDING | ACCEPTED`

## 완료 조건

- [ ] STORES 엔티티/리포지토리 (좌표·영업시간·최소 주문 금액 포함, 복합 인덱스)
- [ ] MENUS 엔티티/리포지토리
- [ ] 거리 계산 공용 유틸 (하버사인 + 도보 80m/분) 및 단위 테스트
- [ ] API 9개 구현, 명세와 요청/응답/에러 코드 일치
- [ ] 가게 목록 조회의 필터 조합·정렬 3종(PRICE_ASC/DISCOUNT_DESC/DISTANCE_ASC) 테스트 (할인 메뉴 없는 가게 후순위 포함)
- [ ] 메뉴 전체 교체의 멱등성·할인가 검증(discountedPrice < price) 테스트
- [ ] 다른 도메인 의존 필드(usableCouponCount, hasUsableCoupon, partnershipStatus)는 기본값 + TODO 처리
- [ ] 서비스 단위 테스트 + 컨트롤러 테스트 통과 (바운딩 박스·거리 정보 포함)
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
