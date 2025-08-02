# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 코드 작업을 수행할 때 필요한 가이드라인과 정보를 제공합니다.

## 프로젝트 개요

### 프로젝트 목적
한국투자증권 OPEN API를 활용하여 주식 정보를 수집하고, 사용자에게 시각화된 정보를 제공하는 웹 서비스 개발을 목표로 합니다.

### 핵심 기능
- **한국투자증권 OPEN API 연동**: 실시간 주식 데이터 수집
- **배치 데이터 수집**: Spring Batch를 통한 대용량 주식 데이터 수집
- **RESTful API 서비스**: 수집된 데이터를 제공하는 API 서버
- **웹 기반 시각화**: React를 이용한 사용자 친화적 대시보드
- **확장 가능한 멀티모듈 구조**: 향후 추가 서비스 개발 고려

### 현재 상태
프로젝트 초기 계획 수립 완료. 멀티모듈 Gradle 프로젝트 구조로 개발 예정.

## 개발 환경

### 시스템 환경
- **운영체제**: macOS (Darwin 24.5.0)
- **개발 도구**: IntelliJ IDEA
- **버전 관리**: Git (저장소 초기화 완료, 아직 커밋 없음)
- **프로젝트 설정**: korea-stock-projects.iml 파일로 IntelliJ 모듈 구성

### 확정된 기술 스택
프로젝트에서 사용할 기술 스택이 다음과 같이 확정되었습니다:

#### 핵심 기술
- **주 개발 언어**: Java
- **빌드 도구**: Gradle (멀티모듈 구조)
- **데이터베이스**: PostgreSQL (무료 오픈소스, 시계열 데이터 최적화)

#### 백엔드 개발
- **Spring Batch**: 한국투자증권 API 데이터 수집 배치 처리
- **Spring Boot WebFlux**: 비동기/반응형 API 서버 (실시간 주식 데이터에 최적화)
- **Spring Data JPA**: 데이터베이스 ORM
- **Spring Security**: 인증 및 보안

#### 프론트엔드 개발
- **React**: 사용자 인터페이스 및 대시보드
- **JavaScript/TypeScript**: 프론트엔드 로직
- **Chart.js 또는 D3.js**: 주식 데이터 시각화

#### 보조 기술
- **Python**: 데이터 분석 및 전처리 (필요시)
- **Redis**: 실시간 데이터 캐싱
- **Docker**: 컨테이너화 및 배포

## 프로젝트 구조 설계

### 멀티모듈 Gradle 구조
```
korea-stock-projects/
├── build.gradle                 # 루트 프로젝트 설정
├── settings.gradle              # 모듈 설정
├── gradle/                      # Gradle 래퍼
├── common/                      # 공통 모듈
│   ├── build.gradle
│   └── src/main/java/
│       └── com/stock/common/    # 공통 유틸, 엔티티, 상수
├── batch-collector/             # 배치 수집 모듈
│   ├── build.gradle
│   └── src/main/java/
│       └── com/stock/batch/
│           ├── config/          # Spring Batch 설정
│           ├── job/             # 배치 Job 정의
│           ├── step/            # 배치 Step 정의
│           ├── reader/          # 한국투자증권 API Reader
│           ├── processor/       # 데이터 가공 Processor
│           └── writer/          # DB Writer
├── api-server/                  # API 서버 모듈
│   ├── build.gradle
│   └── src/main/java/
│       └── com/stock/api/
│           ├── config/          # WebFlux 설정
│           ├── controller/      # REST API 컨트롤러
│           ├── service/         # 비즈니스 로직
│           ├── repository/      # 데이터 액세스
│           └── dto/             # 데이터 전송 객체
├── frontend/                    # React 프론트엔드 모듈
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── components/          # React 컴포넌트
│       ├── pages/               # 페이지 컴포넌트
│       ├── services/            # API 호출 서비스
│       └── utils/               # 유틸리티 함수
├── docs/                        # 문서화
├── docker/                      # Docker 설정 파일
└── scripts/                     # 배포 및 유틸리티 스크립트
```

### 모듈별 역할

#### 1. common 모듈
- **공통 엔티티**: 주식 정보, 사용자 정보 등 JPA 엔티티
- **공통 유틸리티**: 날짜 변환, 데이터 검증 등
- **공통 상수**: API 엔드포인트, 코드 값 등
- **공통 예외**: 커스텀 예외 클래스

#### 2. batch-collector 모듈 (Spring Batch)
- **한국투자증권 API 연동**: OAuth 인증 및 데이터 수집
- **배치 작업 스케줄링**: 정기적 데이터 수집
- **데이터 검증 및 정제**: 수집된 데이터의 무결성 검증
- **데이터베이스 저장**: PostgreSQL에 배치 단위로 저장

#### 3. api-server 모듈 (Spring Boot WebFlux)
- **RESTful API 제공**: 주식 데이터 조회 API
- **실시간 데이터 스트리밍**: WebSocket을 통한 실시간 주가 제공
- **데이터 집계 및 분석**: 차트용 데이터 가공
- **사용자 인증**: JWT 기반 인증 시스템

#### 4. frontend 모듈 (React)
- **대시보드**: 주식 시장 개요 및 주요 지수
- **차트 시각화**: 주가 차트, 거래량 차트 등
- **검색 기능**: 종목 검색 및 상세 정보 조회
- **반응형 웹**: 모바일 및 데스크톱 대응

### 데이터 흐름 아키텍처
1. **수집**: batch-collector → 한국투자증권 API → PostgreSQL
2. **가공**: api-server → PostgreSQL → Redis 캐싱
3. **제공**: React Frontend → api-server → 사용자
4. **실시간**: WebSocket → 실시간 주가 스트리밍

## 기술 선택 근거

### PostgreSQL 선택 이유
1. **무료 오픈소스**: 라이선스 비용 없이 사용 가능
2. **시계열 데이터 최적화**: 주식 데이터의 시간 기반 쿼리 성능 우수
3. **JSON 지원**: 한국투자증권 API JSON 응답을 네이티브로 저장/조회
4. **고성능**: 대용량 데이터 처리 및 복잡한 분석 쿼리 지원
5. **Spring Boot 호환성**: JPA, R2DBC 모두 완벽 지원
6. **확장성**: 파티셔닝, 인덱싱 등 고급 기능 제공

### Spring Boot WebFlux 선택 이유
1. **실시간 스트리밍**: 주가 변동을 실시간으로 전송 (Server-Sent Events, WebSocket)
2. **높은 동시성**: 적은 스레드로 많은 동시 요청 처리
3. **비동기 처리**: 외부 API 호출 시 블로킹 없는 처리
4. **메모리 효율성**: 높은 처리량과 낮은 메모리 사용
5. **Reactive Streams**: 백프레셰어 처리로 시스템 안정성 확보

**주의사항**: 
- Reactive Programming 학습 곡선 존재
- 디버깅 복잡성 증가
- 일부 라이브러리 호환성 제한

## 개발 워크플로우

### 1단계: 프로젝트 기본 구조 설정
1. **멀티모듈 Gradle 프로젝트 생성**: 루트, common, batch-collector, api-server 모듈
2. **공통 의존성 설정**: Spring Boot, Spring Batch, Spring WebFlux, PostgreSQL
3. **데이터베이스 설정**: PostgreSQL 설치 및 스키마 설계
4. **기본 설정 파일**: application.yml, logback.xml 등

### 2단계: 배치 수집 모듈 개발 (batch-collector)
1. **한국투자증권 API 연동**: OAuth2 인증 구현
2. **Spring Batch Job 설계**: 주식 데이터 수집 배치 작업
3. **데이터 수집 스케줄링**: 장 시간 중 실시간 수집, 장 마감 후 일괄 수집
4. **에러 처리 및 재시도**: 네트워크 오류, API 한도 초과 등 예외 처리

### 3단계: API 서버 모듈 개발 (api-server)
1. **WebFlux 기본 설정**: Reactive 웹 스택 구성
2. **데이터 조회 API**: 주식 정보, 차트 데이터 API 개발
3. **실시간 스트리밍**: 실시간 주가 WebSocket 구현
4. **캐싱 전략**: Redis를 통한 응답 속도 최적화

### 4단계: 프론트엔드 모듈 개발 (frontend)
- **TODO**: 추후 계획 수립 예정
- React 기반 대시보드 및 차트 시각화
- 반응형 웹 디자인
- 실시간 데이터 표시

### 5단계: 통합 및 배포
1. **Docker 컨테이너화**: 각 모듈별 Docker 이미지 생성
2. **CI/CD 파이프라인**: GitHub Actions 또는 Jenkins 설정
3. **모니터링 시스템**: 로그 수집, 성능 모니터링
4. **보안 강화**: SSL/TLS, API 인증, 데이터 암호화

## 중요 고려사항

### 데이터 보안 및 컴플라이언스
- **개인정보 보호**: 사용자 데이터 암호화 및 익명화
- **API 키 관리**: 환경 변수 또는 별도 보안 저장소 활용
- **금융 규제 준수**: 금융투자업법, 개인정보보호법 등 관련 법규 준수
- **데이터 사용 권한**: 데이터 제공업체와의 라이선스 협약 확인

### 성능 최적화
- **대용량 데이터 처리**: 분산 처리 및 병렬 처리 기법 활용
- **실시간 데이터**: WebSocket 또는 Server-Sent Events 활용
- **캐싱 전략**: 자주 조회되는 데이터의 효율적 캐싱
- **데이터베이스 최적화**: 인덱싱, 파티셔닝 전략 수립

### 한국 주식 시장 특성
- **거래 시간**: 평일 09:00-15:30 (점심시간 12:00-13:00 제외)
- **가격 제한폭**: 전일 대비 ±30% 제한
- **코스피/코스닥**: 주요 지수 및 섹터별 분류
- **배당 및 권리락**: 한국 고유의 배당 시스템 고려
- **외국인 투자 한도**: 종목별 외국인 지분 제한 고려

## 보안 및 민감 정보 관리

### 제외되는 민감 정보
다음 정보들은 절대 공개 저장소에 커밋되어서는 안 됩니다:
- API 키 및 인증 토큰
- 데이터베이스 연결 정보
- 서버 접속 정보 및 IP 주소
- 사용자 인증 정보
- 거래 계좌 정보
- 개인 투자 데이터

### 권장 보안 관행
- 환경 변수(.env) 파일 사용
- 암호화된 설정 파일 활용
- 정기적인 보안 감사
- 의존성 라이브러리 보안 취약점 모니터링

## 테스트 전략

### 단위 테스트
- 데이터 수집 모듈 테스트
- 분석 함수 정확성 검증
- API 엔드포인트 동작 확인

### 통합 테스트
- 데이터 파이프라인 전체 흐름 테스트
- 외부 API 연동 테스트
- 데이터베이스 연동 테스트

### 성능 테스트
- 대용량 데이터 처리 성능
- 동시 사용자 부하 테스트
- 메모리 사용량 최적화

## 문서화 정책

### 코드 문서화
- 모든 함수와 클래스에 docstring 작성
- 복잡한 알고리즘에 대한 상세 주석
- API 문서 자동 생성 (Swagger/OpenAPI)

### 사용자 문서
- 설치 및 설정 가이드
- 사용법 매뉴얼
- 트러블슈팅 가이드
- FAQ 및 예제 코드

## 프로젝트 개발 명령어

### Gradle 멀티모듈 명령어

#### 전체 프로젝트
```bash
# 전체 프로젝트 빌드
./gradlew build

# 전체 프로젝트 테스트
./gradlew test

# 전체 프로젝트 클린
./gradlew clean

# 의존성 확인
./gradlew dependencies

# 프로젝트 구조 확인
./gradlew projects
```

#### batch-collector 모듈 (Spring Batch)
```bash
# 배치 모듈만 빌드
./gradlew :batch-collector:build

# 배치 모듈 테스트
./gradlew :batch-collector:test

# 배치 작업 실행
./gradlew :batch-collector:bootRun

# 특정 배치 Job 실행
./gradlew :batch-collector:bootRun --args='--job.name=stockDataCollectionJob'

# 배치 jar 생성
./gradlew :batch-collector:bootJar
```

#### api-server 모듈 (Spring Boot WebFlux)
```bash
# API 서버 모듈만 빌드
./gradlew :api-server:build

# API 서버 테스트
./gradlew :api-server:test

# API 서버 실행 (개발 모드)
./gradlew :api-server:bootRun

# API 서버 jar 생성
./gradlew :api-server:bootJar

# API 서버 실행 (프로덕션)
java -jar api-server/build/libs/api-server.jar
```

#### frontend 모듈 (React)
```bash
# 프론트엔드 디렉토리로 이동
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm start

# 프로덕션 빌드
npm run build

# 테스트 실행
npm test

# 린팅 검사
npm run lint

# 타입스크립트 검사 (TypeScript 사용 시)
npm run type-check
```

### 데이터베이스 관련 명령어
```bash
# PostgreSQL 서비스 시작 (macOS)
brew services start postgresql

# PostgreSQL 서비스 중지
brew services stop postgresql

# 데이터베이스 접속
psql -U postgres -d stock_db

# 스키마 마이그레이션 (Flyway 사용 시)
./gradlew flywayMigrate

# 테스트 데이터 초기화
./gradlew flywayClean flywayMigrate
```

### Docker 관련 명령어
```bash
# 전체 서비스 Docker 컨테이너 빌드 및 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d

# 특정 서비스만 실행
docker-compose up postgres redis

# 로그 확인
docker-compose logs -f api-server

# 컨테이너 중지 및 삭제
docker-compose down
```

### 개발 유틸리티 명령어
```bash
# 코드 포매팅 (Spotless 사용 시)
./gradlew spotlessApply

# 코드 스타일 검사
./gradlew checkstyleMain

# 정적 분석 (SpotBugs)
./gradlew spotbugsMain

# 테스트 커버리지 리포트
./gradlew jacocoTestReport

# 전체 품질 검사
./gradlew check
```

### 배포 관련 명령어
```bash
# 프로덕션 빌드 (모든 모듈)
./gradlew clean build -Pprofile=prod

# Docker 이미지 빌드
./gradlew jib

# 스프링 부트 액추에이터 헬스 체크
curl http://localhost:8080/actuator/health

# API 문서 확인 (Swagger UI)
open http://localhost:8080/swagger-ui.html
```

## 모니터링 및 로깅

### 로그 관리
- 구조화된 로깅 (JSON 형태)
- 로그 레벨별 분류 (DEBUG, INFO, WARN, ERROR)
- 민감한 정보 로깅 금지
- 로그 순환 및 보관 정책

### 모니터링 지표
- 시스템 리소스 사용률
- API 응답 시간
- 데이터 수집 성공률
- 에러 발생 빈도
- 사용자 활동 지표

## TODO: 프론트엔드 개발 계획

### React 기반 대시보드 설계
- [ ] **UI/UX 프로토타입 설계**: Figma 또는 Adobe XD를 이용한 화면 설계
- [ ] **컴포넌트 아키텍처 계획**: 재사용 가능한 컴포넌트 구조 설계
- [ ] **상태 관리 전략**: Redux, Zustand, 또는 Context API 선택
- [ ] **라우팅 구조**: React Router를 이용한 페이지 네비게이션

### 주식 데이터 시각화
- [ ] **차트 라이브러리 선택**: Chart.js, D3.js, 또는 TradingView 위젯 평가
- [ ] **실시간 차트 구현**: WebSocket을 통한 실시간 주가 차트
- [ ] **기술적 지표 표시**: 이동평균선, RSI, MACD 등 기술적 분석 지표
- [ ] **반응형 차트**: 모바일 및 데스크톱 환경 대응

### 핵심 기능 구현
- [ ] **종목 검색**: 자동완성 기능이 있는 주식 종목 검색
- [ ] **관심 종목**: 사용자별 관심 종목 즐겨찾기 기능
- [ ] **포트폴리오**: 가상 또는 실제 포트폴리오 관리 기능
- [ ] **알림 시스템**: 가격 알림, 뉴스 알림 등 푸시 알림

### 성능 최적화
- [ ] **코드 스플리팅**: React.lazy를 이용한 번들 크기 최적화
- [ ] **메모이제이션**: React.memo, useMemo, useCallback 적절한 사용
- [ ] **이미지 최적화**: WebP 포맷 사용, 지연 로딩 구현
- [ ] **CDN 적용**: 정적 자원 CDN 배포

### 사용자 경험 개선
- [ ] **로딩 상태**: 스켈레톤 UI 또는 스피너를 통한 로딩 상태 표시
- [ ] **에러 처리**: 사용자 친화적인 에러 메시지 및 폴백 UI
- [ ] **다크 모드**: 다크/라이트 테마 토글 기능
- [ ] **접근성**: WCAG 가이드라인 준수, 키보드 네비게이션 지원

### 배포 및 CI/CD
- [ ] **빌드 최적화**: Webpack 또는 Vite 번들링 최적화
- [ ] **정적 호스팅**: Netlify, Vercel, 또는 AWS S3/CloudFront 배포
- [ ] **자동 배포**: GitHub Actions을 통한 자동 배포 파이프라인
- [ ] **성능 모니터링**: Google Analytics, Lighthouse 성능 측정

### 추가 고려사항
- [ ] **PWA 구현**: Service Worker를 통한 오프라인 지원
- [ ] **국제화(i18n)**: 다국어 지원 (한국어, 영어)
- [ ] **모바일 앱**: React Native 또는 PWA를 통한 모바일 앱 확장
- [ ] **SEO 최적화**: Next.js 고려 또는 SSR 구현

## 한국투자증권 OPEN API 개발 계획

### API 서비스 개요

한국투자증권 OPEN API (https://apiportal.koreainvestment.com/apiservice)를 통해 다음과 같은 서비스를 제공받을 수 있습니다:

#### 제공 서비스
- **국내주식**: 실시간 시세, 주문/계좌 관리, 시장 분석
- **해외주식**: 미국, 중국, 일본 등 해외 주식 거래
- **선물/옵션**: 파생상품 거래 및 시세 정보
- **채권**: 채권 시장 데이터
- **실시간 시세**: WebSocket 기반 실시간 데이터 스트리밍

#### 인증 방식
- **OAuth2 토큰 기반 인증**
- 액세스 토큰 발급 및 갱신
- 실운영/모의투자 환경 지원

### 개발 단계별 계획

#### 1단계: API 연동 환경 구축 (1-2주)

##### 1.1 한국투자증권 개발자 등록 및 API 키 발급
- [ ] 한국투자증권 API 포털 회원가입
- [ ] 앱 등록 및 API Key, Secret Key 발급
- [ ] 모의투자 계좌 개설 (개발/테스트용)
- [ ] API 사용 약관 및 제한사항 확인

##### 1.2 기본 환경 설정
- [ ] API 호출용 HTTP 클라이언트 설정 (WebClient)
- [ ] OAuth2 토큰 관리 시스템 구현
- [ ] 환경별 설정 분리 (개발/테스트/운영)
- [ ] API 호출 로깅 및 모니터링 설정

##### 1.3 기본 API 연동 테스트
- [ ] 토큰 발급/갱신 API 구현
- [ ] 기본 주식 정보 조회 API 테스트
- [ ] API 응답 데이터 파싱 및 검증
- [ ] 에러 처리 및 재시도 로직 구현

#### 2단계: 핵심 주식 데이터 수집 API 구현 (2-3주)

##### 2.1 국내주식 기본 정보 API
- [ ] **주식현재가 시세**: 개별 종목 현재가 조회
- [ ] **주식현재가 호가**: 호가 정보 조회
- [ ] **주식현재가 일자별**: 일봉, 주봉, 월봉 데이터
- [ ] **주식현재가 체결**: 체결 내역 조회
- [ ] **상장종목검색**: 종목 코드 및 기본 정보

##### 2.2 시장 지수 및 순위 정보 API
- [ ] **국내주식 업종별 시세**: KOSPI, KOSDAQ 업종 지수
- [ ] **주식현재가 거래량순위**: 거래량 기준 상위 종목
- [ ] **주식현재가 상승률순위**: 상승률 기준 상위 종목
- [ ] **주식현재가 하락률순위**: 하락률 기준 상위 종목
- [ ] **주식현재가 시가총액순위**: 시가총액 순위

##### 2.3 실시간 데이터 스트리밍 (WebSocket)
- [ ] **실시간 시세**: WebSocket 연결 및 실시간 가격 수신
- [ ] **실시간 호가**: 실시간 호가 정보 스트리밍
- [ ] **실시간 체결**: 실시간 체결 정보 수신
- [ ] 연결 관리 및 재연결 로직 구현

#### 3단계: 데이터 저장 및 배치 처리 시스템 (2-3주)

##### 3.1 데이터베이스 스키마 설계
- [ ] **종목 정보 테이블**: 종목 코드, 이름, 업종, 시장 구분
- [ ] **일별 시세 테이블**: OHLCV(시고저종거래량) 데이터
- [ ] **실시간 시세 테이블**: 실시간 가격 및 호가 정보
- [ ] **시장 지수 테이블**: KOSPI, KOSDAQ 지수 데이터
- [ ] **거래 순위 테이블**: 일별 거래량/상승률 순위

##### 3.2 Spring Batch 작업 구현
- [ ] **일별 시세 수집 Job**: 전 종목 일봉 데이터 수집
- [ ] **종목 정보 갱신 Job**: 신규 상장/폐지 종목 반영
- [ ] **시장 지수 수집 Job**: 주요 지수 데이터 수집
- [ ] **순위 정보 수집 Job**: 거래량/수익률 순위 수집
- [ ] 배치 작업 스케줄링 및 모니터링

##### 3.3 데이터 검증 및 품질 관리
- [ ] 데이터 무결성 검증 로직
- [ ] 중복 데이터 제거 및 정합성 체크
- [ ] 누락 데이터 보정 및 알림 시스템
- [ ] 배치 작업 실패 시 복구 메커니즘

#### 4단계: RESTful API 서버 구현 (2-3주)

##### 4.1 조회 API 개발
- [ ] **종목 검색 API**: 종목명/코드 기반 검색
- [ ] **현재가 조회 API**: 실시간 종목 정보
- [ ] **차트 데이터 API**: 일봉/주봉/월봉 OHLCV 데이터
- [ ] **순위 정보 API**: 거래량/수익률 순위
- [ ] **시장 지수 API**: KOSPI/KOSDAQ 지수 정보

##### 4.2 WebFlux 기반 실시간 API
- [ ] **Server-Sent Events**: 실시간 가격 스트리밍
- [ ] **WebSocket API**: 양방향 실시간 통신
- [ ] **Reactive Streams**: 백프레셔 처리
- [ ] 동시 접속자 관리 및 성능 최적화

##### 4.3 API 문서화 및 보안
- [ ] OpenAPI 3.0 기반 API 문서 자동 생성
- [ ] API 인증 및 권한 관리
- [ ] Rate Limiting 및 API 사용량 제한
- [ ] CORS 설정 및 보안 헤더 적용

#### 5단계: 프론트엔드 대시보드 개발 (3-4주)

##### 5.1 기본 UI 컴포넌트
- [ ] **종목 검색**: 자동완성 기능
- [ ] **종목 상세**: 현재가, 호가, 체결 정보
- [ ] **차트 컴포넌트**: Chart.js 기반 캔들스틱 차트
- [ ] **관심종목**: 즐겨찾기 기능
- [ ] **순위 대시보드**: 거래량/수익률 순위

##### 5.2 실시간 데이터 연동
- [ ] WebSocket 클라이언트 구현
- [ ] 실시간 가격 업데이트
- [ ] 실시간 차트 갱신
- [ ] 알림 및 푸시 기능

##### 5.3 반응형 웹 디자인
- [ ] 모바일 최적화
- [ ] 다크/라이트 테마
- [ ] 접근성 개선
- [ ] 성능 최적화

#### 6단계: 시스템 최적화 및 배포 (2-3주)

##### 6.1 성능 최적화
- [ ] **Redis 캐싱**: 자주 조회되는 데이터 캐싱
- [ ] **데이터베이스 최적화**: 인덱싱, 파티셔닝
- [ ] **API 응답 최적화**: 압축, 페이징
- [ ] **로드 테스트**: 동시 사용자 부하 테스트

##### 6.2 모니터링 및 로깅
- [ ] **애플리케이션 모니터링**: Spring Boot Actuator
- [ ] **로그 수집**: ELK Stack 또는 클라우드 로깅
- [ ] **알람 시스템**: 시스템 장애 시 알림
- [ ] **대시보드**: Grafana 기반 모니터링

##### 6.3 배포 및 운영
- [ ] **Docker 컨테이너화**: 각 모듈별 컨테이너
- [ ] **CI/CD 파이프라인**: GitHub Actions
- [ ] **무중단 배포**: Blue-Green 또는 Rolling 배포
- [ ] **백업 및 복구**: 데이터베이스 백업 전략

### API 호출 제한사항 및 고려사항

#### 호출 제한
- **분당 호출 제한**: API별로 상이 (보통 20-200회/분)
- **일일 호출 제한**: 계약에 따라 상이
- **동시 접속 제한**: WebSocket 연결 수 제한
- **데이터 지연**: 실시간 데이터는 20분 지연 (유료 서비스 제외)

#### 개발 시 주의사항
- **API 키 보안**: 환경 변수 또는 암호화된 설정 파일 사용
- **토큰 관리**: 토큰 만료 시간 추적 및 자동 갱신
- **에러 핸들링**: API 오류 코드별 적절한 처리
- **재시도 로직**: 네트워크 오류 시 지수 백오프 재시도
- **데이터 검증**: API 응답 데이터 무결성 검증

#### 비용 최적화 전략
- **캐싱 전략**: 자주 변하지 않는 데이터 캐싱
- **배치 처리**: 대량 데이터는 배치로 처리
- **선택적 수집**: 필요한 데이터만 선별적 수집
- **압축 전송**: gzip 압축 사용
- **Connection Pooling**: HTTP 연결 재사용

### 예상 개발 일정

| 단계 | 기간 | 주요 산출물 |
|------|------|-------------|
| 1단계 | 1-2주 | API 연동 환경, OAuth2 인증 |
| 2단계 | 2-3주 | 핵심 API 구현, 실시간 연동 |
| 3단계 | 2-3주 | 데이터베이스, 배치 시스템 |
| 4단계 | 2-3주 | RESTful API 서버 |
| 5단계 | 3-4주 | 프론트엔드 대시보드 |
| 6단계 | 2-3주 | 최적화 및 배포 |
| **총 기간** | **12-18주** | **완전한 주식 정보 시스템** |

---

이 문서는 프로젝트 발전에 따라 지속적으로 업데이트되며, 새로운 기능이나 변경사항이 생길 때마다 관련 정보를 추가해야 합니다.