# [1/6] auth 도메인 개발 (+ 프로젝트 초기 세팅)

제휴 쿠폰 서비스 백엔드의 auth 도메인을 개발한다. 첫 번째 도메인이므로 프로젝트 초기 세팅과 공통 인프라도 이 단계에서 함께 만든다.

## 프로젝트 개요

소상공인 제휴 쿠폰 서비스. 사장님(OWNER)은 웹에서 가게를 등록하고 다른 가게와 제휴를 맺어, 결제한 손님에게 제휴 가게의 쿠폰을 QR로 발급한다. 사용자(CUSTOMER)는 앱에서 QR을 스캔해 쿠폰함에 등록하고, 해당 가게에서 사용한다.

- 도메인 6개를 순서대로 개발한다: **auth → store → partnership → coupon → recommendation → statistics**
- 이번 작업 범위는 **auth 도메인 + 초기 세팅**까지다.

## 작업 방식 (중요 — 반드시 지킬 것)

- 이슈 생성, PR 작성, 브랜치 생성/이동/push는 전부 **사용자가 직접** 한다. 너는 절대 하지 마라.
- 작업 시작 전 `git branch --show-current`로 현재 브랜치를 확인해서 알려주고, 그 브랜치에서만 작업한다.
- 이 문서에 적힌 범위까지만 개발한다. 다음 도메인(store)을 미리 시작하지 마라.
- 완료 조건 충족 후: 빌드·테스트 통과를 확인하고, 커밋 목록을 요약해 보고한 뒤 **정지하고 대기**한다.

## 커밋 규칙

- **작은 단위로 자주** 커밋한다. 하나의 커밋 = 하나의 논리적 변경.
- 형식: `타입: 한글 설명` (feat / fix / refactor / test / chore / docs)
  - 예: `chore: 프로젝트 초기 세팅`, `feat: 회원가입 API 구현`, `test: 로그인 서비스 단위 테스트 추가`
- 권장 커밋 단위: 초기 세팅 → 공통 에러 인프라 → 엔티티+리포지토리 → JWT 인프라 → 기능별 (서비스+컨트롤러+테스트를 기능 단위로)

## 기술 스택 및 초기 세팅

- Java 17+, Spring Boot 3.x, Gradle
- Spring Web, Spring Data JPA, Spring Security, Validation
- PostgreSQL (운영), Redis (토큰/임시 데이터)
- 테스트: JUnit 5, 테스트 환경은 Testcontainers(PostgreSQL, Redis) 권장 — H2는 PostgreSQL과 방언 차이가 있어 지양
- JWT 라이브러리 (jjwt 등)

### 공통 에러 응답 (모든 도메인 공통)

`application/problem+json` (RFC 9457) + 커스텀 `code` 필드:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "이미 가입된 이메일입니다",
  "code": "USR_409"
}
```

- 에러 코드는 도메인별 enum으로 관리: `GlobalErrorCode`, `UserErrorCode`, `JwtErrorCode`, `StoreErrorCode`, `PartnershipErrorCode`, `CouponErrorCode` — 공통 인터페이스 `ErrorCode`(code, httpStatus, message)를 구현
- `@RestControllerAdvice` 전역 예외 처리기 + 비즈니스 예외 클래스 1개(ErrorCode를 담는)로 통일
- Bean Validation 실패는 `GLB_400`으로 매핑

### 이번 단계에서 정의할 에러 코드

| type | code | enum | HTTP | message |
|---|---|---|---|---|
| GlobalErrorCode | GLB_400 | INVALID_REQUEST | 400 | 요청 값이 올바르지 않습니다 |
| UserErrorCode | USR_401 | LOGIN_FAILED | 401 | 이메일 또는 비밀번호가 일치하지 않습니다 |
| UserErrorCode | USR_403 | ROLE_NOT_ALLOWED | 403 | 해당 역할로는 수행할 수 없는 요청입니다 |
| UserErrorCode | USR_409 | DUPLICATE_EMAIL | 409 | 이미 가입된 이메일입니다 |
| JwtErrorCode | JWT_400 | MALFORMED_TOKEN | 400 | 토큰 형식이 올바르지 않습니다 |
| JwtErrorCode | JWT_401 | EXPIRED_OR_INVALID_TOKEN | 401 | 토큰이 만료되었거나 유효하지 않습니다 |

## DB 설계 (auth 관련)

### USERS

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(50) | |
| password_hash | VARCHAR(255) | BCrypt |
| email | VARCHAR(50) | UNIQUE |
| role | VARCHAR(20) | CUSTOMER \| OWNER |
| email_verified | BOOLEAN | |
| created_at | DATETIME | |

### Redis — 리프레시 토큰

| key | value | TTL |
|---|---|---|
| `refresh:{userId}` | refreshToken | 1,209,600초 (14일) |

## 인증 방식

- JWT accessToken + refreshToken
- accessToken: `Authorization: Bearer {token}` 헤더. 클레임에 userId, role 포함
- refreshToken: Redis `refresh:{userId}`에 저장된 값과 대조해 검증
- Spring Security 필터에서 JWT 검증, role 기반 인가(OWNER/CUSTOMER). 인가 실패 시 `USR_403`

## API 명세 (4개)

### 1. 회원가입 — `POST /auth/signup`

사용자(앱)와 사장님(웹) 공용 회원가입. role 값으로 구분한다.

Request:
```json
{
  "username": "kimsiheun",
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "role": "CUSTOMER"
}
```
- role: `CUSTOMER | OWNER`

Response 201:
```json
{ "userId": 1 }
```

에러: 400 `GLB_400` (입력값 형식 오류), 409 `USR_409` (이메일 중복)

### 2. 로그인 — `POST /auth/login`

이메일/비밀번호 로그인. accessToken과 refreshToken을 발급하고, refreshToken은 Redis에 저장한다.

Request:
```json
{ "email": "user@example.com", "password": "P@ssw0rd!" }
```

Response 200:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "CUSTOMER"
}
```

에러: 401 `USR_401` (이메일 또는 비밀번호 불일치)

### 3. 로그아웃 — `POST /auth/logout`

Redis에 저장된 refreshToken을 삭제한다.

Request: Header `Authorization: Bearer {accessToken}`

Response 204: Body 없음

에러: 401 `JWT_401`

### 4. 토큰 재발급 — `POST /auth/refresh`

refreshToken으로 accessToken을 재발급한다. Redis의 `refresh:{userId}` 값과 대조한다. (재발급 시 refreshToken도 회전)

Request:
```json
{ "refreshToken": "eyJ..." }
```

Response 200:
```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

에러: 400 `JWT_400` (토큰 형식 오류), 401 `JWT_401` (만료 또는 위조)

## 완료 조건

- [ ] 프로젝트 초기 세팅 (Gradle, 설정 파일, 로컬 실행 가능)
- [ ] 공통 에러 인프라 (ErrorCode 인터페이스, 비즈니스 예외, 전역 핸들러, problem+json 응답)
- [ ] USERS 엔티티/리포지토리, Redis 연동
- [ ] JWT 발급/검증 + Security 필터 체인 (role 인가 포함)
- [ ] API 4개 구현, 명세와 요청/응답/에러 코드 일치
- [ ] 서비스 단위 테스트 + 컨트롤러 테스트 통과
- [ ] 전체 빌드 성공 확인 후 보고하고 정지
