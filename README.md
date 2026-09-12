# GetAPI

개발자가 자신의 API를 등록하고 공유할 수 있는 **API 마켓플레이스** 플랫폼입니다.  
API 등록자는 자신의 API를 프록시 형태로 제공하고, 사용자는 포인트를 충전해 원하는 API를 호출합니다.

---

## 주요 기능

- **API 라이브러리** — 등록된 API 목록 조회, 검색, 필터링 (제목/설명/작성자/해시태그)
- **API 프록시** — `X-GetAPI-Key` 인증 + HMAC 서명 검증 후 원본 API로 투명하게 포워딩
- **포인트 결제** — 토스페이먼츠 연동, API 호출 시 포인트 자동 차감
- **AI 자동 검열** — 등록·수정 시 혐오표현 감지 AI 서버로 자동 검수 (`isCensored` 처리)
- **OAuth2 로그인** — Google 소셜 로그인 + JWT + Redis Refresh Token
- **커뮤니티** — 게시글 작성·댓글·좋아요
- **SMS 인증** — 휴대폰 번호 인증
- **어드민 페이지** — 유저·API 관리, 검열 처리
- **HATEOAS** — API 응답에 `_links` 자동 주입 옵션

---

## 기술 스택

| 분류 | 사용 기술 |
|---|---|
| Backend | Spring Boot 3.3, Spring Security, Spring Data JPA |
| 인증 | OAuth2 (Google), JWT, Redis |
| 템플릿 | Thymeleaf + Layout Dialect |
| DB | H2 (개발), Hibernate DDL Auto |
| 검색 | Elasticsearch 8.13 |
| 결제 | 토스페이먼츠 (OpenFeign) |
| AI 검열 | FastAPI + BERT ([GetAPI_Censored_Ai](https://github.com/devlib-itsw/GetAPI_Censored_Ai)) |
| 메일 | Spring Integration Mail (IMAP) |
| 빌드 | Gradle |

---

## 요구사항

- Java 17 이상
- Redis
- Elasticsearch 8.x (선택)
- [GetAPI_Censored_Ai](https://github.com/devlib-itsw/GetAPI_Censored_Ai) AI 서버 (선택)
- Google OAuth2 클라이언트 ID/Secret
- 토스페이먼츠 시크릿 키 (결제 기능 사용 시)

---

## 환경 설정

`src/main/resources/application.properties` 또는 `.env` 파일에 아래 값을 설정합니다.

```properties
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
jwt.secret=YOUR_JWT_SECRET

# 토스페이먼츠
payment.toss.secret-key=YOUR_TOSS_SECRET_KEY

# AI 검열 서버 (기본값: http://localhost:8888)
ai.server.url=http://localhost:8888

# Elasticsearch (선택)
spring.elasticsearch.uris=http://localhost:9200
```

---

## 실행

```bash
# 의존성 설치 및 빌드
./gradlew build

# 실행
./gradlew bootRun
```

서버가 `http://localhost:8080`에서 시작됩니다.

H2 콘솔은 `http://localhost:8080/h2-console`에서 접근 가능합니다.

---

## 프로젝트 구조

```
src/main/java/com/getapi/
├── admin/          # 어드민 페이지 (유저·API 관리)
├── ai/             # AI 검열 서버 연동
├── api/            # API 등록·수정·삭제·조회
├── auth/           # OAuth2, JWT, SMS 인증
├── comment/        # API 댓글, 게시글 댓글
├── errors/         # 글로벌 에러 처리
├── global/         # 공통 설정, 스케줄러
├── library/        # API 라이브러리 목록·검색
├── payments/       # 토스페이먼츠 결제
├── post/           # 커뮤니티 게시글·좋아요
├── proxy/          # API 프록시 (호출 로그 포함)
├── tag/            # 해시태그
└── user/           # 회원 정보·프로필·포인트
```

---

## API 프록시 사용법

등록된 API는 `/lib/{slug}/...` 경로로 호출합니다.

```http
POST /lib/{slug}/endpoint
X-GetAPI-Key: YOUR_API_KEY
X-GetAPI-Timestamp: 1714000000000
X-GetAPI-Signature: HMAC_SHA256_SIGNATURE
```

| 헤더 | 설명 |
|---|---|
| `X-GetAPI-Key` | 발급받은 API Key |
| `X-GetAPI-Timestamp` | Unix timestamp (ms) |
| `X-GetAPI-Signature` | HMAC-SHA256 서명 |

호출 시 포인트가 자동 차감되며, 호출 로그가 대시보드에 기록됩니다.

---

## 브랜치 전략

| 브랜치 | 역할 |
|---|---|
| `main` | 배포용 안정 브랜치 |
| `develop` | 통합 개발 브랜치 |
| `{이름}` | 개인 개발 브랜치 |

---

## 관련 레포지토리

- [GetAPI_Censored_Ai](https://github.com/devlib-itsw/GetAPI_Censored_Ai) — 한국어 혐오표현 감지 AI 서버

---
Team DevLib · 팀장 [노정원 (njwon)](https://njw.kro.kr)
