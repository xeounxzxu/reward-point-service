# Reward Point Service

포인트 적립, 적립취소, 사용, 사용취소를 원장 중심으로 관리하는 Spring Boot 서비스입니다.

현재 구조는 `pointaccount`, `pointcore`, `pointearn`, `pointuse` 기준의 feature + layered architecture 를 사용합니다.

## 설명

이 프로젝트는 아래 요구사항을 기준으로 설계되었습니다.

- 1회 최대 적립 가능 포인트와 개인 최대 보유 가능 포인트를 정책으로 제어
- 적립 lot 단위로 사용 이력과 취소 이력을 추적
- 수기 지급 포인트를 일반 적립과 구분하고 우선 사용
- 사용취소 시 원 적립분이 만료되었으면 신규 적립으로 재발행
- 멀티 인스턴스 환경을 고려해 같은 `accountId` 에 대한 포인트 변경 요청은 계정 단위 DB 비관적 락(`PESSIMISTIC_WRITE`) 사용

현재 구현 범위는 다음 API 를 포함합니다.

- `POST /api/point-accounts`
- `GET /api/point-accounts/{accountId}`
- `POST /api/points/earn`
- `POST /api/points/earn-cancel`
- `POST /api/points/use`
- `POST /api/points/use-cancel`

## 기술 스택

- Java 21
- Spring Boot 3.5.13
- Spring Data JPA
- H2
- Spring REST Docs

## 빌드 방법

### 1. 전체 테스트 포함 빌드

```bash
./gradlew clean build
```

### 2. 테스트 실행

```bash
./gradlew test
```

### 3. REST Docs 생성

```bash
./gradlew test asciidoctor
```

생성 결과:

- HTML 문서: `build/docs/asciidoc/index.html`
- snippet: `build/generated-snippets/`

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 Spring Boot 기본값을 따릅니다.

## 주요 설계 포인트

### 원장 중심 설계

- `point_transaction` 은 모든 포인트 변경 이벤트의 공통 헤더입니다.
- `point_earn_lot` 은 실제 사용 가능한 적립 단위입니다.
- `point_use_allocation` 은 어떤 적립 lot 가 어떤 주문 사용에 소진되었는지 기록합니다.
- `point_use_cancel_allocation` 은 사용취소 시 원복 또는 재적립 여부를 기록합니다.

### 동시성 전략

- 포인트 변경 유스케이스는 `PointAccount` 를 동시성 경계로 사용합니다.
- 적립, 적립취소, 사용, 사용취소는 `PointAccountRepository.findByIdForUpdate()` 로 계정을 먼저 잠급니다.
- 멀티 인스턴스 환경에서도 같은 `accountId` 요청은 DB 비관적 락(`PESSIMISTIC_WRITE`)을 사용합니다.

### 패키지 구조

- `pointaccount`: 포인트 계정 생성/조회
- `pointcore`: 공통 거래, 정책, 예외, 공통 결과 모델
- `pointearn`: 포인트 적립, 적립취소, 만료 처리
- `pointuse`: 포인트 사용, 사용취소, 사용 배분 이력

## 다이어그램


