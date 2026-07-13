# [3/6] partnership 도메인 개발

제휴 쿠폰 서비스 백엔드의 partnership 도메인을 개발한다. auth, store 도메인은 이미 구현되어 있다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 이 문서에 적힌 범위까지만 개발한다. 다음 도메인(coupon)을 미리 시작하지 마라.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `feat: 제휴 요청 API 구현`, `feat: 제휴 수락/거절 API 구현`
- 권장 커밋 단위: 엔티티+리포지토리 → 기능별 (서비스+컨트롤러+테스트) → 기존 코드 연동(가게 검색 partnershipStatus)

## 기존 공통 규칙 (이미 구현됨 — 그대로 따를 것)

- 에러 응답: `application/problem+json` + `code` 필드. 기존 `ErrorCode` 인터페이스/전역 핸들러 재사용
- 인증: `Authorization: Bearer {accessToken}`, role 인가. 역할 위반 `USR_403`, 입력값 오류 `GLB_400`, 가게 없음 `STR_404`

### 이번 단계에서 추가할 에러 코드 (PartnershipErrorCode)

| code | enum | HTTP | message |
|---|---|---|---|
| PTN_403 | NOT_PARTNERSHIP_PARTY | 403 | 해당 제휴의 당사자가 아닙니다 |
| PTN_404 | PARTNERSHIP_NOT_FOUND | 404 | 존재하지 않는 제휴입니다 |
| PTN_409_01 | PARTNERSHIP_ALREADY_EXISTS | 409 | 이미 진행 중이거나 체결된 제휴입니다 |
| PTN_409_02 | INVALID_PARTNERSHIP_STATUS | 409 | 현재 상태에서 처리할 수 없는 요청입니다 |

## DB 설계 — PARTNERSHIPS

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| requester_store_id | BIGINT FK → STORES | 요청 가게 |
| receiver_store_id | BIGINT FK → STORES | 수신 가게 |
| status | VARCHAR(20) | PENDING \| ACCEPTED \| REJECTED \| TERMINATED |
| created_at | DATETIME | |
| accepted_at | DATETIME | |

- **제약: `UNIQUE(requester_store_id, receiver_store_id)`**

## 상태 전이 규칙

```
PENDING ──accept──> ACCEPTED ──terminate──> TERMINATED
   └──reject──> REJECTED
```

- accept/reject는 **수신 가게 소유주만**, PENDING 상태에서만 가능
- terminate는 **양쪽 가게 모두** 가능, ACCEPTED 상태에서만 가능
- 상태 전이 위반은 `PTN_409_02`, 당사자 아님은 `PTN_403`

## API 명세 (5개)

### 1. 제휴 요청 — `POST /partnerships` (OWNER)

내 가게가 다른 가게에 제휴를 요청한다. 생성 시 상태는 PENDING.

Request: `Authorization: Bearer {accessToken}` (OWNER)
```json
{ "receiverStoreId": 12 }
```

Response 201:
```json
{ "partnershipId": 5, "status": "PENDING" }
```

에러: 404 `STR_404` (대상 가게 없음), 409 `PTN_409_01` (이미 진행 중/체결된 제휴, UNIQUE 제약)

### 2. 제휴 목록 조회 — `GET /partnerships` (OWNER)

내 가게 관련 제휴 목록. 받은 요청과 보낸 요청 모두 포함.

Query: `status`(선택) = PENDING | ACCEPTED | REJECTED | TERMINATED

Response 200:
```json
{
  "partnerships": [
    {
      "partnershipId": 5,
      "partnerStore": { "storeId": 12, "name": "흔카페", "category": "CAFE" },
      "direction": "RECEIVED",
      "status": "PENDING",
      "createdAt": "2026-07-13T10:00:00"
    }
  ]
}
```
- direction: `SENT | RECEIVED`

### 3. 제휴 수락 — `PATCH /partnerships/{partnershipId}/accept` (OWNER, 수신 가게 소유주)

PENDING → ACCEPTED. accepted_at 기록.

Response 200:
```json
{ "partnershipId": 5, "status": "ACCEPTED", "acceptedAt": "2026-07-13T11:00:00" }
```

에러: 403 `PTN_403`, 404 `PTN_404`, 409 `PTN_409_02` (PENDING 아님)

### 4. 제휴 거절 — `PATCH /partnerships/{partnershipId}/reject` (OWNER, 수신 가게 소유주)

PENDING → REJECTED.

Response 200:
```json
{ "partnershipId": 5, "status": "REJECTED" }
```

에러: 403 `PTN_403`, 404 `PTN_404`, 409 `PTN_409_02`

### 5. 제휴 해지 — `DELETE /partnerships/{partnershipId}` (OWNER, 제휴 당사자)

ACCEPTED → TERMINATED. 양쪽 가게 모두 해지 가능. 이미 발급된 쿠폰은 유효기간까지 사용 가능(쿠폰에는 영향 없음).

Response 204: Body 없음

에러: 403 `PTN_403`, 404 `PTN_404`, 409 `PTN_409_02` (ACCEPTED 아님)

## 기존 코드 연동

- store 도메인의 **제휴 대상 가게 검색**(`GET /stores/search`)에서 TODO로 남긴 `partnershipStatus`를 실제 값으로 채운다: 내 가게와 대상 가게 간 제휴가 없으면 NONE, PENDING이면 PENDING, ACCEPTED이면 ACCEPTED (REJECTED/TERMINATED는 NONE 취급 — 재요청 가능해야 하므로. 단 UNIQUE 제약과의 충돌은 기존 행 재사용으로 처리)

## 완료 조건

- [ ] PARTNERSHIPS 엔티티/리포지토리 (+ UNIQUE 제약)
- [ ] API 5개 구현, 상태 전이 규칙·에러 코드 명세와 일치
- [ ] 가게 검색 partnershipStatus 연동
- [ ] 상태 전이/권한 검증 단위 테스트 + 컨트롤러 테스트 통과
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
