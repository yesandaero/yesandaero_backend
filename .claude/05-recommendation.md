# [5/6] recommendation 도메인 개발

제휴 쿠폰 서비스 백엔드의 recommendation 도메인을 개발한다. auth, store, partnership, coupon 도메인은 이미 구현되어 있다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 이 문서에 적힌 범위까지만 개발한다. 다음 도메인(statistics)을 미리 시작하지 마라.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `feat: 가게 추천 조회 API 구현`, `test: 추천 정렬 기준 테스트 추가`

## 기존 공통 규칙 (이미 구현됨 — 그대로 따를 것)

- 에러 응답: `application/problem+json` + `code` 필드. 입력값 오류는 `GLB_400`
- 인증: `Authorization: Bearer {accessToken}`, CUSTOMER 인가
- 새 에러 코드 없음. 새 테이블 없음 (STORES, COUPONS, COUPON_TEMPLATES 조회만)

## API 명세 (1개)

### 가게 추천 — `GET /recommendations/stores` (CUSTOMER)

금액 제한과 음식 카테고리 필터로 가게를 추천한다. 사용자가 보유한 REGISTERED(만료 전) 쿠폰을 사용할 수 있는 가게를 상단에 우선 노출한다. 응답의 좌표로 리스트와 지도 핀을 함께 렌더링한다. lat/lng/radius를 주면 해당 반경 내 가게만 추천한다.

Query:
- `maxPrice`: 1인 평균 가격 상한 (필수, 예: 10000)
- `category` (선택, 복수 가능): KOREAN | CHINESE | JAPANESE | WESTERN | SNACK | CAFE
- `lat, lng, radius` (선택): 기준 좌표와 반경(m), 현재 위치 주변 추천용. lat/lng 전달 시 각 가게에 거리 정보(distanceMeters, walkingMinutes) 포함 — store 도메인의 거리 계산 공용 유틸(하버사인, 도보 80m/분) 재사용, 없으면 null
- `page, size`

**정렬: `hasUsableCoupon DESC, avgPrice ASC`**

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
      "hasUsableCoupon": true,
      "usableCoupons": [ { "couponId": 101, "name": "아메리카노 1000원 할인" } ]
    },
    {
      "storeId": 15,
      "name": "유성분식",
      "category": "SNACK",
      "avgPrice": 7000,
      "latitude": 36.3541,
      "longitude": 127.3412,
      "openTime": "11:00",
      "closeTime": "20:00",
      "minOrderAmount": 7000,
      "distanceMeters": 1250,
      "walkingMinutes": 16,
      "hasUsableCoupon": false,
      "usableCoupons": []
    }
  ],
  "page": 0,
  "totalPages": 5
}
```

에러: 400 `GLB_400` (maxPrice 누락 또는 음수)

## 구현 참고

- "사용 가능 쿠폰" 기준은 coupon 도메인과 동일: 해당 가게가 혜택 제공 가게(템플릿 store)이고, 내 쿠폰 status = REGISTERED, 만료 전
- 가게 목록 + 쿠폰 매칭 시 N+1 금지 — 쿠폰을 한 번에 조회해 메모리 매핑하거나 조인으로 처리
- radius 필터는 하버사인 공식 또는 바운딩 박스 근사(선필터) + 정확 거리 계산으로 구현

## 완료 조건

- [ ] API 1개 구현, 필터·정렬·페이징 명세와 일치
- [ ] 정렬 기준(쿠폰 보유 가게 우선, 평균 가격 오름차순) 테스트
- [ ] radius 필터 테스트
- [ ] N+1 없는 쿼리 확인
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
