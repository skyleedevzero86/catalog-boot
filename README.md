<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/4121d61c-6780-4bd5-8c15-1df5d7ccd7ff" />

<br/>

# DataBridge Catalog Control

**DataBridge Catalog Control**은 분석계 데이터 적재 파이프라인의 **연결 정보, 메타데이터, DDL 변환, 적재 작업 이력**을 관리하는 백엔드 제어 서버입니다.

이 프로젝트는 데이터를 직접 대량으로 처리하는 Worker 엔진이 아닙니다.
원천 DB와 타깃 DB 사이에서 **어떤 연결을 사용할지, 어떤 테이블을 대상으로 할지, 어떤 방식으로 적재할지, 작업 결과를 어떻게 추적할지**를 REST API로 관리하는 **Control Plane** 역할을 합니다.

---

## 개요

```
[운영자 / Admin UI / 외부 시스템]
        │
        │ REST API
        ▼
┌────────────────────────────────────────────┐
│   DataBridge Catalog Control               │
│   - 연결 프로필 관리                       │
│   - 메타데이터 카탈로그 관리               │
│   - DDL 변환 및 적재 작업 제어             │
│   - 작업 상태 및 이력 관리                 │
│                                            │
│   PostgreSQL Control DB                    │
│   schema: databridge                       │
└───────────────────┬────────────────────────┘
                    │ JDBC
                    ▼
[원천 DB] Oracle / PostgreSQL / MySQL / MariaDB / ClickHouse
                    │
                    │ DDL 생성 / 데이터 적재
                    ▼
[타깃 DB] PostgreSQL / MySQL / MariaDB / ClickHouse / 분석계 DB

                    └── Optional: Extract Worker HTTP 연동
```

---

## 핵심 역할

| 영역             | 설명                                                  |
| -------------- | --------------------------------------------------- |
| **DB 연결 관리**   | 원천·타깃 DB 접속 정보를 등록하고, JDBC 기반 연결 상태를 점검합니다.         |
| **메타데이터 카탈로그** | 원천 DB의 테이블, 컬럼, 코드 유형 정보를 수집하고 조회할 수 있게 관리합니다.      |
| **DDL 변환**     | 원천 테이블 구조를 타깃 DB에 맞는 CREATE TABLE 문으로 변환하고 미리보기합니다. |
| **데이터 적재 제어**  | 단일 테이블 적재 또는 다중 테이블 배치 적재 작업을 생성하고 실행합니다.           |
| **작업 운영**      | jobId 기준으로 작업 상태, 테이블별 성공/실패, 처리 건수, 재시도 여부를 추적합니다. |
| **Worker 연동**  | 필요 시 별도 Extract Worker에 HTTP 방식으로 추출 작업을 위임합니다.     |

---

## 이 프로젝트의 범위

### 포함하는 기능

* 원천/타깃 DB 연결 프로필 관리
* JDBC 연결 헬스체크
* 원천 DB 메타데이터 동기화
* 테이블/코드 유형/카테고리 관리
* DDL 변환 미리보기
* 단일 테이블 동기 적재
* 다중 테이블 비동기 배치 적재
* 작업 상태 조회, 취소, 실패 건 재시도
* Extract Worker 연동 API

### 포함하지 않는 기능

* 회원가입, 로그인, 권한 관리 UI
* 프론트엔드 화면
* Airflow, Spark 수준의 대용량 분산 처리 엔진
* 파일 기반 벌크 로더 전용 기능
* 실시간 스트리밍 파이프라인

즉, 이 프로젝트는 **데이터를 직접 무겁게 처리하는 엔진**이 아니라
**데이터 적재 작업을 등록하고, 실행을 제어하고, 결과를 추적하는 백엔드 관리 계층**입니다.

---

## 사용 시나리오

예를 들어, **Oracle HR 스키마를 PostgreSQL 분석계 DB로 이관**해야 한다고 가정합니다.

1. **연결 등록**
   Oracle 원천 DB와 PostgreSQL 타깃 DB의 JDBC 프로필을 등록합니다.

2. **메타데이터 동기화**
   Oracle 스키마의 테이블, 컬럼, 코드 유형 정보를 수집해 Control DB에 저장합니다.

3. **DDL 미리보기**
   `EMPLOYEES` 테이블이 PostgreSQL에서는 어떤 CREATE TABLE 문으로 변환되는지 확인합니다.

4. **적재 실행**
   단일 테이블은 동기 방식으로, 여러 테이블은 비동기 배치 방식으로 적재합니다.

5. **작업 모니터링**
   jobId를 기준으로 전체 상태, 테이블별 성공/실패, 처리 행 수를 조회합니다.

6. **실패 재처리**
   실패한 테이블만 골라 재시도합니다.

---

## 기능 맵

| 영역         | 기능                              | 대표 API                         |
| ---------- | ------------------------------- | ------------------------------ |
| **DB 연결**  | 연결 프로필 CRUD, JDBC ping          | `/api/v1/conn/*`               |
| **메타 조회**  | 테이블 목록, 코드 유형 목록 조회             | `/api/v1/meta/tables/{mtdtId}` |
| **메타 동기화** | 원천 DB introspection 후 메타 테이블 저장 | `POST /api/v1/meta/sync`       |
| **카테고리**   | 메타 테이블 분류 트리 및 매핑 관리            | `/api/v1/category/*`           |
| **마이그레이션** | DDL 변환, 단일/배치 적재, job 운영        | `/api/v1/migration/*`          |
| **추출 연동**  | Extract Worker 작업 제출/취소/상태 조회   | `/api/v1/extract/*`            |

지원 DB:

* PostgreSQL
* MySQL
* MariaDB
* Oracle
* ClickHouse

---

## 기술 구조

| 계층           | 사용 기술                               |
| ------------ | ----------------------------------- |
| API          | Spring Boot 3, REST API, Swagger UI |
| Control DB   | PostgreSQL, Flyway                  |
| Persistence  | MyBatis                             |
| 조회 모델        | SQL View                            |
| 명령 처리        | Stored Procedure                    |
| ETL Adapter  | JDBC 기반 원천 조회 및 타깃 DDL/INSERT       |
| ID 채번        | DB 함수 기반 업무 ID 생성                   |
| Architecture | Hexagonal Architecture              |

---

## 데이터 접근 방식

조회와 명령 처리를 분리합니다.

### 조회

조회 API는 SQL View를 통해 필요한 데이터를 읽습니다.

예시:

* `v_lnkg_profile`
* `v_mtdt_tbl_list`
* `v_mig_job`
* `v_mig_job_table`

### 등록/수정/삭제

등록, 수정, 삭제 작업은 Stored Procedure를 통해 처리합니다.

예시:

* `sp_lnkg_profile`
* `sp_mtdt_sync`
* `sp_category`
* `sp_mig_job`

Procedure 호출 시 `op` 값을 기준으로 작업을 구분합니다.

| op  | 의미     |
| --- | ------ |
| `C` | Create |
| `U` | Update |
| `D` | Delete |

이 방식은 업무 규칙과 감사 컬럼 처리를 DB 레벨에서 일관되게 유지하기 위한 선택입니다.

---

## 아키텍처

이 프로젝트는 헥사고날 아키텍처를 기준으로 구성합니다.

```
Controller
   ↓
Application Service
   ↓
Domain
   ↓
Port
   ↓
Adapter
```

패키지 예시:

```
databridge.catalog
├── domain
├── application
├── adapter
│   ├── inbound
│   │   └── web
│   └── outbound
│       ├── persistence
│       ├── jdbc
│       └── worker
└── global
```

각 계층의 책임은 다음과 같습니다.

| 계층                  | 책임                               |
| ------------------- | -------------------------------- |
| Controller          | REST API 요청/응답 처리                |
| Application Service | 유스케이스 흐름 제어                      |
| Domain              | 업무 규칙, 상태 코드, 핵심 모델              |
| Port                | 외부 의존성 추상화                       |
| Adapter             | DB, JDBC, HTTP Worker 등 실제 연동 구현 |

---

## 프로젝트 정보

| 항목           | 값                                       |
| ------------ | --------------------------------------- |
| Gradle 프로젝트명 | `databridge-catalog-control`            |
| 패키지          | `databridge.catalog`                    |
| 메인 클래스       | `DataBridgeCatalogApplication`          |
| 기본 포트        | `8081`                                  |
| API prefix   | `/api/v1`                               |
| 설정 prefix    | `databridge.catalog.*`                  |
| Swagger UI   | `http://localhost:8081/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs`     |

---

## API 요약

상세 요청/응답 예시는 Swagger UI에서 확인합니다.

### DB 연결 API

Base path: `/api/v1/conn`

| Method | Path                          | 설명        |
| ------ | ----------------------------- | --------- |
| POST   | `/conn/create`                | 연결 프로필 등록 |
| POST   | `/conn/update/{connectionId}` | 연결 프로필 수정 |
| POST   | `/conn/delete/{connectionId}` | 연결 프로필 삭제 |
| GET    | `/conn/list`                  | 연결 목록 조회  |
| GET    | `/conn/detail/{connectionId}` | 연결 상세 조회  |

---

### 메타데이터 API

Base path: `/api/v1/meta`

| Method | Path                                   | 설명              |
| ------ | -------------------------------------- | --------------- |
| GET    | `/meta/tables/{mtdtId}`                | 메타 테이블 목록 조회    |
| GET    | `/meta/code-types/{mtdtId}`            | 코드 유형 목록 조회     |
| GET    | `/meta/code-types/{mtdtId}/candidates` | 코드 유형 등록 후보 조회  |
| POST   | `/meta/sync`                           | 원천 DB 메타데이터 동기화 |

---

### 카테고리 API

Base path: `/api/v1/category`

| Method | Path                               | 설명             |
| ------ | ---------------------------------- | -------------- |
| POST   | `/category/create/{mtdtId}`        | 카테고리 생성        |
| POST   | `/category/update/{categoryId}`    | 카테고리 수정        |
| POST   | `/category/delete/{categoryId}`    | 카테고리 삭제        |
| POST   | `/category/map-table/{categoryId}` | 카테고리-테이블 매핑 교체 |
| GET    | `/category/list/{mtdtId}`          | 카테고리 목록 조회     |

---

### 마이그레이션 API

Base path: `/api/v1/migration`

| Method | Path                             | 설명                    |
| ------ | -------------------------------- | --------------------- |
| POST   | `/migration/ddl/preview`         | CREATE TABLE DDL 미리보기 |
| POST   | `/migration/load`                | 단일 테이블 동기 적재          |
| POST   | `/migration/load/batch`          | 다중 테이블 비동기 배치 적재      |
| GET    | `/migration/jobs`                | 작업 목록 조회              |
| GET    | `/migration/jobs/{jobId}`        | 작업 상태 조회              |
| GET    | `/migration/jobs/{jobId}/tables` | 테이블별 진행 상태 조회         |
| POST   | `/migration/jobs/{jobId}/cancel` | 작업 취소                 |
| POST   | `/migration/jobs/{jobId}/retry`  | 실패 작업 재시도             |

---

### 추출 연동 API

Base path: `/api/v1/extract`

| Method | Path                                 | 설명                      |
| ------ | ------------------------------------ | ----------------------- |
| POST   | `/extract/submit`                    | Extract Worker에 작업 제출   |
| POST   | `/extract/cancel/{extractRequestId}` | Extract Worker 작업 취소    |
| GET    | `/extract/status/{extractRequestId}` | Extract Worker 작업 상태 조회 |

---

## 작업 상태 코드

| 상태                | 의미        |
| ----------------- | --------- |
| `PENDING`         | 작업 대기     |
| `RUNNING`         | 작업 실행 중   |
| `SUCCESS`         | 전체 성공     |
| `PARTIAL_SUCCESS` | 일부 테이블 실패 |
| `FAILED`          | 전체 실패     |
| `CANCELLED`       | 작업 취소     |

---

## 실행 방법

```powershell
cd DataBridge_Main
.\gradlew.bat test
.\gradlew.bat bootRun
```

기본 실행 조건:

* PostgreSQL Control DB 필요
* 기본 DB: `localhost:5432/databridge`
* 기본 schema: `databridge`
* API 인증은 PoC 기준 비활성화
* Swagger UI에서 API 직접 호출 가능

---

## 감사 컬럼 처리

CUD API는 선택적으로 `userId` 헤더를 받을 수 있습니다.

```http
userId: admin
```

헤더가 없는 경우 기본값은 `system`으로 처리합니다.

이를 통해 API 호출 주체를 감사 컬럼에 기록할 수 있습니다.

---

## 문서

| 문서                             | 설명             |
| ------------------------------ | -------------- |
| `docs/PRD.md`                  | 제품 요구사항 문서     |
| `docs/appendix/api-catalog.md` | REST API 상세 명세 |

---

## 정리

DataBridge Catalog Control은 분석계 데이터 적재를 위한 **메타데이터 중심 제어 서버**입니다.

핵심은 단순합니다.

* DB 연결을 등록한다.
* 원천 DB의 메타데이터를 수집한다.
* 타깃 DB에 맞는 DDL을 확인한다.
* 적재 작업을 실행한다.
* 작업 결과를 추적하고 실패 건을 재처리한다.

즉, 이 프로젝트는 데이터 파이프라인의 실행 엔진이라기보다
**데이터 이관 작업을 안정적으로 운영하기 위한 관리·제어 백엔드**입니다.
