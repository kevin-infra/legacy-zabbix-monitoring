# ④ 레거시 Zabbix 모니터링 (이건영)

> ※ 팀 공통 주제(서울시 공공데이터 기반 지역추천서비스 — AI Agent + 망분리 고도화) 중 **Kevin 개인 단독 담당 파트**입니다. 10/7 팀 주제 변경 이후 Kevin의 역할은 "기프티콘 증정 서비스" 전체 3-Tier 구현에서 **"레거시(폐쇄망) 환경에 대한 Zabbix 모니터링" 단독 구축**으로 좁혀졌으며(조장님 10/7·10/8 확인), 다른 조원 파트와는 서버·IP·일정 연동이 전혀 없는 완전 독립 파트입니다.

## 1. 개요

팀의 TO-BE 아키텍처는 **폐쇄망(내부망)**(회원 WAS + 코어 WAS, Spring Boot 3.5, Oracle 23ai)과 **외부망(서비스망)**(React 19 + AI Agent + PostgreSQL 18)을 **망연계(마스킹 게이트웨이)**로 연결하는 구조입니다. 이 중 AI Agent·추천 로직·공공데이터 처리·망분리 구조 설계·망연계 구현은 모두 다른 조원이 담당하며, 팀 구성도에는 "※ 모니터링 도구: 구성 중"이라는 빈 자리가 있습니다. **Kevin이 담당하는 것은 바로 이 자리 — 폐쇄망(레거시) 환경에 대한 Zabbix 모니터링 체계 전체**입니다.

Kevin은 실제 금융사(현대카드) 인프라 모니터링 운영 경력(JENNIFER, ZABBIX)을 보유하고 있어, 이 파트는 실무 경력을 가장 직접적으로 증빙할 수 있는 영역입니다. 모니터링 대상 서버(웹서버 + Spring Boot 2 WAS)부터 트래픽 폭주·CPU/메모리 과부하 장애 시나리오 설계, Zabbix 설치·트리거 구성, 가이드 문서화까지 전 과정을 혼자 설계·구축합니다.

## 2. 주요 범위

- **모니터링 대상 인프라 구축**: VM 3대 — ① 웹서버(리버스 프록시) ② Spring Boot 2 WAS(팀의 최신화 "회원 WAS"에 대응하는 레거시 역할, 인증/로그인 트래픽 시나리오와 연결) ③ Zabbix Server(+ Java Gateway + Web UI)
- **3단 모니터링 체계**: OS 레벨(CPU/메모리/프로세스, Zabbix Agent2) + JVM 레벨(힙/스레드/GC, Java Gateway+JMX) + 애플리케이션 레벨(HTTP 응답시간/에러율, Spring Boot Actuator)
- **장애/부하 시나리오 설계**: "트래픽 몰림 → CPU/메모리 과부하 → 응답 지연 → 장애 감지"를 `ab`/`siege`/`wrk`로 재현 — 콘서트 티켓팅 트래픽 폭주 대응 경험과 연결되는 스토리로 구체화
- **임계치 트리거 설계 및 알림 테스트**: CPU 80%, 메모리 85% 등 기준으로 트리거 3종 이상 강제 발생 및 알림 확인
- **자동화**: Ansible Playbook으로 Zabbix Agent 설치/설정 반복 작업 자동화 (쉘 스크립트가 아닌 Ansible 선택 — Kevin 실제 교육과정 기준)
- **(후속, 조건부)** Oracle DB — DB 담당 조원에게서 전달받은 뒤 Kevin이 직접 설치 + Zabbix Oracle 모니터링 연동
- **기술문서화**: `docs/zabbix_monitoring_guide.md` 작성 — 조장님 요청에 따라 약 100페이지 분량의 상세 기술문서(설치 과정, 시나리오 설계 근거, 트러블슈팅 포함)

## 3. 인프라 목표

| 구분 | 내용 | 우선순위 |
|---|---|---|
| 서버 구성 | 웹서버 / Spring Boot 2 WAS / Zabbix Server, VM 3대로 분리 구축 (조장님 10/8 확정 지시) | 필수 |
| Spring Boot 2 WAS | 최소 Hello World + CPU/메모리 소모 API 1~2개만 구현 (비즈니스 로직 최소화, 리눅스 중심 설계 원칙) | 필수 |
| OS 레벨 모니터링 | Zabbix Agent2 설치 및 호스트 등록, CPU/메모리/프로세스 감시 | 필수 |
| JVM 레벨 모니터링 | Zabbix Java Gateway + JMX 연동, 힙메모리/스레드/GC 감시 | 필수 |
| 애플리케이션 레벨 모니터링 | Spring Boot Actuator + HTTP 에이전트 아이템, 응답시간/에러율 감시 | 필수 |
| 장애/부하 시나리오 | 트래픽 폭주 재현(`ab`/`siege`/`wrk`) → 과부하 → 트리거 발생 → 알림 확인 | 필수 |
| 자동화 | Ansible Playbook으로 Zabbix Agent 설치/설정 자동화 (수동 완료 후 적용) | 필수 |
| Oracle DB 연동 | DB 담당 조원에게서 전달받아 설치 + Zabbix Oracle 모니터링(Agent2 플러그인 또는 UserParameter) | 후속(조건부) |
| 기술문서 | `docs/zabbix_monitoring_guide.md`, 약 100페이지 목표 (조장님 요청) | 필수 |

> ※ 망분리 구조(폐쇄망/외부망) 자체의 설계·구축, AI Agent·추천 로직, 공공·부동산 데이터 처리, 망연계(마스킹 게이트웨이) 구현은 모두 다른 조원 담당이며 Kevin 파트와 연동 없음.

## 4. 기술 스택 (Kevin 파트 전용)

- OS: Rocky Linux 8 (레거시 고정, 팀의 최신화 Rocky Linux 10과 대응되는 레거시 선택)
- 웹서버: Apache 또는 nginx (리버스 프록시)
- WAS: Spring Boot 2 (Java, JDK 11) — 팀의 최신화 "회원 WAS"(Spring Boot 3.5)에 대응하는 레거시 역할
- DB: (후속) Oracle — 팀의 최신화 Oracle 23ai에 대응, DB 담당 조원에게서 전달받아 설치
- 모니터링: Zabbix Server + Agent2 + Java Gateway + Web UI (최신 LTS 버전)
- 부하 테스트: `ab` / `siege` / `wrk`
- 자동화: Ansible (수동 설치 → Ansible Playbook 자동화, 2단계 구조)
- 가상화: VMware Workstation Pro 26.0.0(build 25388281) + Vagrant 2.4.9(`vagrant-vmware-desktop` 플러그인 3.0.5)로 VM 3대(`legacy-web`/`legacy-was`/`legacy-zabbix`) 자동 생성·관리 — **(10/9 정정)** `vagrant-vmware-desktop` 플러그인은 2021년에 무료/오픈소스로 전환되어 유료 아님(10/8 "유료라 배제" 판단은 오정보였음을 확인). VMware 26과 구버전 Vagrant VMware Utility 간 레지스트리 경로 불일치 문제를 보완 설치로 해결한 뒤, 기존 GUI 수동 생성 VM 3대를 삭제하고 Vagrantfile(멀티머신 구성) 기반으로 재생성. 게스트 박스: `generic/rocky8` 4.3.12 (내부 OS: Rocky Linux 8.9 Green Obsidian)
- Zabbix Server 설치 방식: **(10/9 재정정)** Docker Compose가 아니라 **VM에 직접(RPM/수동) 설치** — 10/9 조장님이 "Zabbix Server는 반드시 실제 VM 위에서 돌아가야 한다"고 재확인함에 따라, 바로 위 줄의 "Docker Compose 승인" 기록은 같은 날 안에 다시 뒤집힌 것으로 정정
- 작업 PC: 강의실 Windows PC 단일 환경 (Kevin 개인 Mac은 Apple Silicon 호환 문제로 이번 범위에서 미사용, 원격 작업 시 크롬 원격 데스크톱 + PowerShell 내장 `ssh` 클라이언트로 직접 접속. Vagrant는 VMware Workstation과 연동해서 VM 생성·관리 용도로 사용 — 10/8 "미사용" 판단에서 10/9 전환)
- 버전관리: GitHub (`kevin-infra/legacy-zabbix-monitoring`), 저장소 안 `vagrant/` 하위 폴더에 `Vagrantfile` 보관 (`.gitignore`로 `.vagrant/` 실제 VM 파일은 제외, 저장소에는 재현 가능한 설정 파일만 커밋)

## 5. 작업 원칙

- 모든 실습은 명령어 단위로 진행하며, 명령어 하나를 입력할 때마다 그 명령어를 왜 쓰는지, 각 옵션이 무엇을 의미하는지, 어떤 목적으로 이 시점에 입력하는지를 함께 기록하고 이해한 뒤 다음 단계로 넘어감
- 개발자 과정이 아닌 인프라 과정 수강생 기준 — Spring Boot 2 앱 자체(비즈니스 로직)는 최소화하고, 리눅스 기초(프로세스/리소스 관리, systemd, 방화벽/SELinux, 성능 분석 명령어)에 설계·학습 비중을 둠
- 과도하게 복잡한 패턴은 지양하고, 모니터링 핵심 로직(트리거·알림·시나리오)에 집중
- 수동으로 원리를 이해한 뒤, 반복되는 설치/설정 작업만 Ansible Playbook으로 자동화 (처음부터 자동화 도구에 의존하지 않음)
- 다른 조원 파트와는 서버·IP·일정 연동이 없는 완전 독립 파트이므로, 외부 의존성 없이 스스로 진행 가능한 순서로 일정을 짠다 (단, Oracle DB 전달 시점은 예외)
- **(10/9 추가)** 작업을 Claude에게 지시하기 전, "이 결과물을 조장님/PM이 보면 뭐라고 할까 → 어느 지점에서 피드백받을까 → 더 나은 방법은 뭘까"를 먼저 점검한다. 특히 학습용 상세 기록(명령어 한 줄씩 설명)과 실제 결과물 증빙(백오피스 화면 등)은 구분해서, 반복 설치 문서화에 시간을 과도하게 쓰지 않도록 주의

## 6. 일정 (총 19일, 10/8~10/26)

### 1단계: 인프라 구축 (10/8~10/10, 3일)
| 날짜 | 할 일 |
|---|---|
| 10/8 (목) | Vagrantfile 작성, VM 3대(웹서버/Spring Boot 2 WAS/Zabbix Server) 기동 — Rocky Linux 8 box, IP/리소스 할당 |
| 10/9 (금) | 웹서버(Apache/nginx) 설치 및 리버스 프록시 설정, Spring Boot 2 최소 앱(Hello World + 부하용 API 1~2개) 배포, **(신규) `legacy-was`에 Docker+JDK+Maven 설치 후 AS-IS 회원 서비스(`old-corebank`, Docker+MySQL 5.7 기반) 설치·구동 테스트** — 조장님이 Jira(KAN-21)로 10/9 당일 추가 지시 |
| 10/10 (토) | 웹서버→WAS 프록시 통신 점검, `ab`/`siege`/`wrk` 부하 테스트 도구 설치 및 동작 확인 |

### 2단계: Zabbix 설치 및 3단 모니터링 연동 (10/11~10/15, 5일)
| 날짜 | 할 일 |
|---|---|
| 10/11 (일) | `legacy-zabbix`에 Docker 설치 → **Docker Compose로 Zabbix Server+DB+Web UI 구동**(RPM 수동 설치 대신 속도 우선 — 10/9 조장님 피드백 반영), Java Gateway는 필요 시 별도 컨테이너로 추가 |
| 10/12 (월) | Zabbix Web UI 접속 확인("백오피스" 화면 — 조장님 요청 기준점), 웹서버·WAS에 Agent2 설치(`dnf install`) 및 호스트 등록(OS 레벨) |
| 10/13 (화) | Spring Boot 2 JMX 옵션 적용(`-Dcom.sun.management.jmxremote`), Java Gateway 연동, "Generic Java JMX" 템플릿 적용(JVM 레벨) |
| 10/14 (수) | Spring Boot Actuator 의존성 추가, `/actuator/metrics`·`/actuator/health` 노출, HTTP 에이전트 아이템 구성(애플리케이션 레벨) |
| 10/15 (목) | 3단 모니터링(OS/JVM/애플리케이션) 데이터 수집 통합 점검 |

### 3단계: 장애 시나리오 설계·재현 및 알림 테스트 (10/16~10/18, 3일)
| 날짜 | 할 일 |
|---|---|
| 10/16 (금) | 임계치 트리거 설계(CPU 80%, 메모리 85%, JVM 힙 초과, HTTP 응답시간 지연 등) |
| 10/17 (토) | 트래픽 폭주 시나리오(로그인/인증 요청 집중) 설계 및 부하 테스트 실행 |
| 10/18 (일) | 트리거 발생 확인, 알림 테스트, "트래픽 몰림→과부하→지연→감지" 전체 흐름 재구성 |

### 4단계: 자동화 (10/19~10/20, 2일)
| 날짜 | 할 일 |
|---|---|
| 10/19 (월) | Ansible Playbook 작성 — Zabbix Agent 설치/설정 자동화 |
| 10/20 (화) | Playbook 반복 실행 테스트 및 보완 |

### 5단계: 후속 작업 및 문서화 (10/21~10/26, 6일)
| 날짜 | 할 일 |
|---|---|
| 10/21 (수) | (조건부) Oracle DB 전달받는 시점이면 설치 진행 + Zabbix Oracle 모니터링 연동 착수. 아직 전달 전이면 기술문서 작성 선행 |
| 10/22 (목) | 기술문서(`docs/zabbix_monitoring_guide.md`) 초안 — 설치 전 사전조사, 설치 과정 기록 |
| 10/23 (금) | 기술문서 — 시나리오/트리거 설계 근거, 트러블슈팅 기록 |
| 10/24 (토) | 기술문서 — 장애 재현 Before/After, 결론 및 향후 개선 방향 (약 100p 분량 목표 점검) |
| 10/25 (일) | 전체 점검, 발표자료(캡처·수치) 정리 |
| 10/26 (월) | 최종 점검 — **Kevin 파트 구현 완료** |

> Oracle DB 수신 시점은 DB 담당 조원에게 달려 있어 고정되지 않음 — 받으면 즉시 5단계에 끼워 넣고, 그 전까지는 1~4단계와 문서화를 먼저 진행.

## 7. 예상 리스크 및 대응

- **Oracle DB 수신 시점 불확실** → Oracle 연동은 "후속(조건부)" 작업으로 분리하고, 받기 전까지는 웹서버/WAS/Zabbix 3대 구축과 JMX/Actuator 연동, 트리거/시나리오, Ansible 자동화를 먼저 완료해 전체 일정이 Oracle 수신에 막히지 않도록 함
- **Spring Boot 2 심화 학습 부담** → 인프라 과정 수강생 기준으로 비즈니스 로직은 최소화(Hello World + 부하 API 1~2개)하고, 학습·설계 비중은 리눅스 기초(systemd, 방화벽/SELinux, 성능 분석 명령어)에 집중
- **Oracle Zabbix 연동 난이도(기본 패키지 미지원)** → Agent2 Oracle 플러그인 또는 UserParameter+sqlplus 방식 중 명령어 중심인 UserParameter 방식을 우선 검토 (PRD 05 4-1절 참고)
- **작업 환경 제약(맥북 Apple Silicon에서 Rocky Linux 8 미지원)** → 강의실 Windows PC 단일 환경으로 고정, 주말 등 원격 작업 시 크롬 원격 데스크톱 + headless VM + `vagrant ssh`로 부담 최소화
- **100페이지 기술문서 분량 확보** → 설치 전 사전조사, 명령어 단위 출력 기록, 트러블슈팅 기록까지 처음부터 상세하게 작성하는 방식으로 분량을 자연스럽게 확보 (PRD 05 8절 참고)
- **일정 부족 시 우선순위** → 1~4단계(VM구축, Zabbix 3단 모니터링, 트리거/시나리오, Ansible)를 먼저 끝내고, 밀리면 Oracle 연동 후속 작업과 문서 분량 확장을 뒤로 미룸

## 8. 기대 효과

- 실무 모니터링 경력(JENNIFER/ZABBIX, 금융사 인프라 운영)을 Zabbix 구축 전 과정(설치·트리거·시나리오·자동화)으로 실증적으로 재현
- OS 레벨+JVM 레벨+애플리케이션 레벨 3단 모니터링 체계를 직접 설계·구축한 경험으로 APM/인프라 모니터링 역량을 포트폴리오화
- 트래픽 폭주 대응 실무 경험(콘서트 티켓팅)을 장애 시나리오로 구체화해 재현·증빙함으로써 "경험 기반 설계"의 스토리 확보
- Ansible을 활용한 반복 가능한 모니터링 에이전트 배포 자동화 경험 확보
- 리눅스 기초(프로세스/리소스 관리, systemd, 방화벽/SELinux)부터 Zabbix 설치·운영까지 직접 수행한 시스템엔지니어 직무 밀착형 포트폴리오 확보
