# 도메인별 개발 프롬프트 사용법

제휴 쿠폰 서비스 백엔드(Spring Boot + JPA + PostgreSQL + Redis)를 도메인 단위로 개발하기 위한 프롬프트 6개입니다.

## 진행 순서 (의존성 기준)

| 순서 | 파일 | 도메인 | API 수 |
|---|---|---|---|
| 1 | `01-auth.md` | auth (+ 프로젝트 초기 세팅) | 4 |
| 2 | `02-store.md` | store (메뉴·영업시간·최소주문·거리·목록 조회 포함) | 9 |
| 3 | `03-partnership.md` | partnership | 5 |
| 4 | `04-coupon.md` | coupon (사용은 버튼 즉시 처리) | 7 |
| 5 | `05-recommendation.md` | recommendation | 1 |
| 6 | `06-statistics.md` | statistics | 1 |

## 사이클

1. 내가 이슈 생성 → 브랜치 생성 → 체크아웃
2. 해당 도메인 프롬프트를 Claude에게 붙여넣기
3. Claude가 개발 (작은 단위로 자주 커밋) 후 완료 보고하고 정지
4. 내가 확인 → push → PR 생성
5. 다음 도메인으로 1부터 반복

## 참고

- 각 프롬프트는 자체 완결형입니다. 공통 규칙 + 해당 도메인 명세가 모두 들어 있어 하나만 붙여넣으면 됩니다.
- 명세 원본: Notion API 명세서 / DB 설계 (1) / 에러 코드 (1)
- Notion ERD·API 명세서에 반영 완료: STORES에 latitude/longitude, open_time/close_time, min_order_amount 추가, MENUS 테이블 신설, 메뉴 일괄 수정 API(PUT /stores/{storeId}/menus) 페이지 생성, 상세/지도/추천 응답에 거리(distanceMeters, walkingMinutes)·영업시간·최소 주문 금액 추가
