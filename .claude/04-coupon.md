# [4/6] coupon 도메인 개발

제휴 쿠폰 서비스 백엔드의 coupon 도메인을 개발한다. auth, store, partnership 도메인은 이미 구현되어 있다. 이 서비스의 핵심 도메인이며 API가 7개로 가장 크다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 이 문서에 적힌 범위까지만 개발한다. 다음 도메인(recommendation)을 미리 시작하지 마라.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `feat: 쿠폰 템플릿 생성 API 구현`, `feat: 쿠폰 발급 QR 토큰 생성 구현`
- 권장 커밋 단위: 템플릿 엔티티 → 템플릿 API 3개 → 쿠폰 엔티티 → 발급/등록 → 쿠폰 사용 → 쿠폰함 → 기존 코드 연동

## 기존 공통 규칙 (이미 구현됨 — 그대로 따를 것)

- 에러 응답: `application/problem+json` + `code` 필드. 기존 `ErrorCode` 인터페이스/전역 핸들러 재사용
- 인증: `Authorization: Bearer {accessToken}`, role 인가. 역할 위반 `USR_403`, 입력값 오류 `GLB_400`

### 이번 단계에서 추가할 에러 코드 (CouponErrorCode)

| code | enum | HTTP | message |
|---|---|---|---|
| CPN_401 | INVALID_COUPON_TOKEN | 401 | 쿠폰 토큰이 만료되었거나 유효하지 않습니다 |
| CPN_403_01 | ISSUE_NOT_ALLOWED | 403 | 쿠폰 발급 권한이 없습니다 |
| CPN_403_02 | NOT_COUPON_OWNER | 403 | 본인의 쿠폰이 아닙니다 |
| CPN_404_01 | TEMPLATE_NOT_FOUND | 404 | 존재하지 않는 쿠폰 템플릿입니다 |
| CPN_404_02 | COUPON_NOT_FOUND | 404 | 존재하지 않는 쿠폰입니다 |
| CPN_409_01 | COUPON_ALREADY_REGISTERED | 409 | 이미 등록된 쿠폰입니다 |
| CPN_409_02 | INVALID_COUPON_STATUS | 409 | 사용할 수 없는 상태의 쿠폰입니다 |

## DB 설계

### COUPON_TEMPLATES

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| store_id | BIGINT FK → STORES | **템플릿 소유(발급) 가게** |
| name | VARCHAR(100) | |
| discount_type | VARCHAR(20) | AMOUNT \| RATE |
| discount_value | INT | RATE는 1~100 |
| min_order_amount | INT | |
| valid_days | INT | 등록 후 유효 일수 |
| active | BOOLEAN | |
| created_at | DATETIME | |

### COUPONS

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| template_id | BIGINT FK → COUPON_TEMPLATES | |
| issuer_store_id | BIGINT FK → STORES | **발급한 가게** (템플릿 소유 가게) |
| target_store_id | BIGINT FK → STORES | **사용처(제휴 가게)** — 발급 시 지정 |
| user_id | BIGINT FK → USERS | 등록 전 NULL |
| used_store_id | BIGINT FK → STORES | 사용 전 NULL, 사용 시 target_store_id로 기록 |
| status | VARCHAR(20) | ISSUED \| REGISTERED \| USED \| EXPIRED |
| issued_at / registered_at / used_at / expires_at | DATETIME | |

### Redis — 1회용 토큰

| key | value | TTL | 용도 |
|---|---|---|---|
| `coupon:issue:{token}` | couponId | 600초 | 발급 QR (사용자가 스캔해 등록) |

## 도메인 개념 & 상태 전이

- 사장님 A가 **본인 가게의 템플릿**을 만들고, 결제 손님에게 발급할 때 **사용처로 제휴(ACCEPTED) 가게 B를 지정**한다 → 손님이 B에 가서 사용 (상부상조 구조). 손님에게 제휴 가게로 갈 소비 명분을 만들어주는 것이 서비스의 핵심 목적이다.
- 쿠폰의 사용처는 발급 시점에 `target_store_id`로 고정된다.
- 상태 전이: `ISSUED ──등록──> REGISTERED ──사용──> USED`, 만료 시 `EXPIRED`
- **사용 플로우는 단순화됨**: 사용자가 앱 쿠폰함에서 사용 버튼을 누르면 즉시 USED 처리 (매장에서 직원 확인 하에 누르는 것을 전제). 사용용 QR/코드, redeem 토큰은 없다.
- **동시성/멱등성**: 등록은 Redis 토큰 삭제(GETDEL 등 원자 연산), 사용은 상태 조건부 UPDATE(`WHERE status = 'REGISTERED'`)로 원자적·멱등하게 처리한다.

## API 명세 (7개)

### 1. 쿠폰 템플릿 생성 — `POST /coupon-templates` (OWNER)

사장님이 **본인 가게의 쿠폰 템플릿**을 생성. 발급도 본인이 하며, 발급 시 사용처로 지정한 제휴 가게에서 사용된다.

Request:
```json
{
  "name": "아메리카노 1000원 할인",
  "discountType": "AMOUNT",
  "discountValue": 1000,
  "minOrderAmount": 5000,
  "validDays": 14
}
```

Response 201:
```json
{ "templateId": 3 }
```

에러: 400 `GLB_400` (할인값 범위 오류, RATE는 1~100), 403 `USR_403`

### 2. 쿠폰 템플릿 수정/비활성화 — `PATCH /coupon-templates/{templateId}` (OWNER, 템플릿 소유 가게)

비활성화 시 신규 발급 중단, 이미 발급된 쿠폰은 유효.

Request:
```json
{ "active": false }
```

Response 200: 수정된 템플릿 전체 반환

에러: 403 `STR_403` (템플릿 소유 가게 아님), 404 `CPN_404_01`

### 3. 내 쿠폰 템플릿 목록 — `GET /coupon-templates` (OWNER)

내 가게의 템플릿 목록. 결제 후 발급 화면에서 **내 템플릿 + 사용처(제휴 ACCEPTED 가게)**를 선택할 때 사용.

Query: `active`(선택) = true | false

Response 200:
```json
{
  "templates": [
    {
      "templateId": 3,
      "name": "아메리카노 1000원 할인",
      "discountType": "AMOUNT",
      "discountValue": 1000,
      "minOrderAmount": 5000,
      "validDays": 14,
      "active": true
    }
  ]
}
```

### 4. 쿠폰 발급 (QR 생성) — `POST /coupons/issue` (OWNER)

결제 완료 후 사장님 웹에서 발급. **본인 가게의 활성 템플릿**을 선택하고 **사용처(targetStoreId)로 제휴(ACCEPTED) 가게를 지정**한다. 쿠폰(ISSUED) 생성 시 `target_store_id` 고정 + 1회용 등록 토큰 QR 반환. 토큰은 Redis `coupon:issue:{token}` TTL 600초.

Request:
```json
{ "templateId": 3, "targetStoreId": 12 }
```
- templateId: 본인 가게의 활성 템플릿
- targetStoreId: 내 가게와 제휴(ACCEPTED) 상태인 가게

Response 201:
```json
{
  "couponId": 101,
  "qrPayload": "couponapp://register?token=8f3a...",
  "expiresIn": 600
}
```

에러: 403 `CPN_403_01` (내 템플릿 아님, 비활성 템플릿, 또는 사용처가 제휴 상태 아님), 404 `CPN_404_01` (템플릿 없음) / `STR_404` (대상 가게 없음)

### 5. 쿠폰 등록 (QR 스캔) — `POST /coupons/register` (CUSTOMER)

QR 스캔으로 내 쿠폰함에 등록 (ISSUED → REGISTERED). 토큰 검증 후 Redis 키 삭제로 중복 등록 방지. **멱등 처리**. expires_at = 등록 시점 + validDays. 응답의 `store`는 **사용처(발급 시 지정된 제휴 가게)**다.

Request:
```json
{ "token": "8f3a..." }
```

Response 200:
```json
{
  "couponId": 101,
  "name": "아메리카노 1000원 할인",
  "store": { "storeId": 12, "name": "흔카페" },
  "status": "REGISTERED",
  "expiresAt": "2026-07-27T23:59:59"
}
```

에러: 401 `CPN_401` (토큰 만료/위조), 409 `CPN_409_01` (이미 등록된 쿠폰)

### 6. 내 쿠폰함 조회 — `GET /coupons/me` (CUSTOMER)

Query: `status`(선택) = REGISTERED | USED | EXPIRED

Response 200:
```json
{
  "coupons": [
    {
      "couponId": 101,
      "name": "아메리카노 1000원 할인",
      "store": { "storeId": 12, "name": "흔카페", "category": "CAFE" },
      "status": "REGISTERED",
      "expiresAt": "2026-07-27T23:59:59"
    }
  ]
}
```

### 7. 쿠폰 사용 — `POST /coupons/{couponId}/use` (CUSTOMER)

사용자가 앱 쿠폰함에서 **사용 버튼을 눌러** 즉시 사용 처리 (REGISTERED → USED). 매장에서 직원 확인 하에 누르는 것을 전제. 본인 쿠폰만 가능하며, `used_store_id`는 발급 시 지정된 사용처(`target_store_id`)로 기록한다. 상태 전이는 조건부 UPDATE로 원자적·멱등 처리.

Request: `Authorization: Bearer {accessToken}` (CUSTOMER), Body 없음

Response 200:
```json
{
  "couponId": 101,
  "name": "아메리카노 1000원 할인",
  "discountType": "AMOUNT",
  "discountValue": 1000,
  "status": "USED",
  "usedAt": "2026-07-13T12:30:00"
}
```

에러: 403 `CPN_403_02` (본인 쿠폰 아님), 404 `CPN_404_02`, 409 `CPN_409_02` (이미 사용/만료된 쿠폰)

## 기존 코드 연동

- **가게 상세 조회** (`GET /stores/{storeId}`)의 TODO `usableCouponCount`: 해당 가게에서 사용 가능한(`target_store_id` = 해당 가게, status = REGISTERED, 만료 전) 내 쿠폰 개수로 채운다
- **지도 영역 내 가게 조회** (`GET /stores/map`)의 TODO `hasUsableCoupon`: 같은 기준으로 채운다 (N+1 주의 — 한 번에 조회)
- 만료 처리: 조회 시점 기준 expires_at 지난 REGISTERED 쿠폰은 EXPIRED로 간주/전이 (스케줄러는 선택)

## 완료 조건

- [ ] COUPON_TEMPLATES, COUPONS 엔티티/리포지토리
- [ ] Redis 1회용 발급 토큰 저장/원자적 소비 유틸
- [ ] API 7개 구현, 명세와 요청/응답/에러 코드 일치
- [ ] 등록/사용의 원자성·멱등성 테스트 (중복 요청 시나리오 포함)
- [ ] 발급 검증 테스트: 내 템플릿이 아닌 경우·비활성 템플릿·사용처가 제휴(ACCEPTED) 아닌 경우 403
- [ ] store 도메인 TODO 필드(usableCouponCount, hasUsableCoupon) 연동
- [ ] 서비스 단위 테스트 + 컨트롤러 테스트 통과
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
