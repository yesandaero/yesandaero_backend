# [6/6] statistics 도메인 개발

제휴 쿠폰 서비스 백엔드의 statistics 도메인을 개발한다. 마지막 도메인이며, auth, store, partnership, coupon, recommendation은 이미 구현되어 있다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `feat: 가게 발급/사용 통계 API 구현`

## 기존 공통 규칙 (이미 구현됨 — 그대로 따를 것)

- 에러 응답: `application/problem+json` + `code` 필드
- 인증: `Authorization: Bearer {accessToken}`, OWNER 인가
- 새 에러 코드 없음 (기존 `STR_403`, `STR_404` 재사용). 새 테이블 없음 (COUPONS 집계 조회만)

## API 명세 (1개)

### 가게 발급/사용 통계 — `GET /stores/{storeId}/statistics` (OWNER, 본인 가게)

사장님 웹 대시보드용 통계. 내 가게가 **발급한** 쿠폰과 내 가게에서 **사용된** 쿠폰을 제휴 가게별로 집계한다. 상부상조 효과(어느 가게가 손님을 보내줬는지)를 확인하는 용도.

Query: `from`, `to` (ISO 8601 날짜)

Response 200:
```json
{
  "period": { "from": "2026-07-01", "to": "2026-07-13" },
  "issued": { "total": 120, "registered": 80, "used": 45 },
  "redeemedAtMyStore": {
    "total": 30,
    "byIssuerStore": [
      { "storeId": 12, "name": "흔카페", "count": 18 },
      { "storeId": 15, "name": "유성분식", "count": 12 }
    ]
  }
}
```

에러: 403 `STR_403` (본인 가게 아님), 404 `STR_404` (가게 없음)

## 집계 기준

- `issued`: `issuer_store_id = 내 가게`인 쿠폰. 기간 기준은 issued_at
  - `total` = 기간 내 발급 전체, `registered` = 그중 등록까지 간 것, `used` = 그중 사용까지 간 것
- `redeemedAtMyStore`: `used_store_id = 내 가게`, status = USED인 쿠폰. 기간 기준은 used_at
  - `byIssuerStore`: 발급 가게(issuer_store_id)별 그룹핑, count 내림차순
- 집계는 DB 레벨(GROUP BY)로 처리 — 애플리케이션에서 전체 로드 금지

## 완료 조건

- [ ] API 1개 구현, 응답 구조·집계 기준 명세와 일치
- [ ] 기간 필터·가게별 그룹핑 집계 쿼리 테스트
- [ ] 권한 검증(본인 가게만) 테스트
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
