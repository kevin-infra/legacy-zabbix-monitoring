# 05. 레거시 — Zabbix 모니터링 구축 + 가이드 문서화

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조. 최신 확정 사항은 `착수보고서_Zabbix모니터링_이건영.md`를 1차 소스로 참조할 것 — 이 PRD는 그 내용을 "무엇을 왜 하는지" 관점으로 풀어 쓴 것이고, 날짜·세부 결정이 바뀌면 착수보고서 쪽이 먼저 갱신되고 이 PRD도 뒤따라 갱신되어야 함.

## 0. ⚠️ 확인 필요 (10/10 아침 재확인 권장)

착수보고서(`착수보고서_Zabbix모니터링_이건영.md`) 안에서도 같은 날(10/9) 안에 번복된 결정이 있어, 다음 2가지는 Kevin이 직접 "현재 기준"을 재확인한 뒤 이 PRD에 반영해야 함:
1. **VM 생성 방식**: CLAUDE.md(10/8 기록)는 "VirtualBox+Vagrant → VMware Workstation GUI 수동 생성"으로 전환했다고 되어 있으나, 착수보고서(10/9 정정)는 "`vagrant-vmware-desktop` 플러그인이 무료로 확인되어 다시 Vagrantfile 기반 자동 생성으로 복귀"라고 되어 있음. 작업 로그상 실제로는 `vagrant ssh legacy-was` 등 Vagrant 명령을 계속 사용 중이므로, 현재는 **후자(Vagrant 복귀)가 실제 상태**로 보이나 CLAUDE.md 쪽이 아직 이걸 반영 못함.
2. **Zabbix Server 설치 방식**: 착수보고서 내에서 "Docker Compose로 구동" → 같은 날 "조장님이 VM에 직접 설치해야 한다고 재확인"으로 다시 번복됨. 아직 실제 설치 전이므로, 착수에 들어가기 전에 조장님께 한 번 더 확인 권장.

## 1. 목표
레거시(폐쇄망) 환경에 Zabbix Server/Agent2/Java Gateway를 설치하고, OS/JVM/애플리케이션 3단 모니터링 체계와 임계치 기반 트리거를 구성해 장애 발생 시 알림이 오도록 한다. 추가로 팀/본인이 참고할 **Zabbix 모니터링 가이드 문서**(`docs/zabbix_monitoring_guide.md`, 조장님 요청 기준 약 100페이지 목표)를 병행 작성한다.

## 2. 배경/컨텍스트
10/7 팀 주제가 "서울시 공공데이터 기반 지역추천서비스(AI Agent)"로 변경되면서, Kevin의 담당 범위는 팀 전체 아키텍처 중 **"폐쇄망(레거시) 환경에 대한 Zabbix 모니터링" 단독 구축**으로 좁혀졌다(조장님 10/7·10/8 확인). 망분리 구조(폐쇄망/외부망) 자체의 설계·구축, AI Agent·추천 로직, 공공데이터 처리, 망연계(마스킹 게이트웨이)는 모두 다른 조원 담당이며 Kevin 파트와 서버·IP·일정 연동이 없는 완전 독립 파트다.

Kevin의 실무 경력(현대카드 인프라 모니터링, JENNIFER/ZABBIX 운영)과 직접 연결되는 영역으로, 모니터링 대상 서버 구축부터 장애 시나리오 설계, Zabbix 설치·트리거 구성, 가이드 문서화까지 전 과정을 혼자 설계·구축한다.

## 3. 범위
- **In-Scope**: VM 3대(웹서버 / Spring Boot 2 WAS / Zabbix Server) 구축, Zabbix Server+Agent2+Java Gateway+Web UI 설치, OS/JVM/애플리케이션 3단 모니터링 연동, 임계치 트리거 설계 및 알림 테스트, 트래픽 폭주 장애 시나리오 재현(`ab`/`siege`/`wrk`), Ansible Playbook으로 Agent 설치 자동화, (후속·조건부) Oracle DB 전달받아 설치 + Zabbix Oracle 모니터링 연동, 가이드 문서 작성
- **Out-of-Scope**: 망분리 구조(폐쇄망/외부망) 설계·구축, AI Agent·추천 로직, 공공데이터 처리, 망연계(마스킹 게이트웨이), 최신화 환경 모니터링(팀 공통 LGTM 스택 — 다른 조원 담당)

## 4. 요구사항

### 4-1. 인프라 구축
1. VM 3대 구성 확정(조장님 10/8 지시): ① 웹서버(Apache/nginx, 리버스 프록시) ② Spring Boot 2 WAS(JDK 11, Hello World + 부하용 API 1~2개만 — 비즈니스 로직 최소화) ③ Zabbix Server(+Java Gateway+Web UI)
2. OS: Rocky Linux 8 고정. IP 대역은 팀과 맞출 필요 없음(완전 독립)
3. (후속·조건부) Oracle DB는 Kevin이 임의 구성하지 않고, DB 담당 조원에게서 전달받은 뒤 설치 + Zabbix Oracle 모니터링 연동(Agent2 플러그인 또는 UserParameter+sqlplus 검토)

### 4-2. Zabbix 설치 및 3단 모니터링
4. Zabbix Server, Agent2, Java Gateway, Web UI 설치(설치 방식은 §0-2 확인 후 확정)
5. 웹서버·WAS VM에 Zabbix Agent2 설치 및 호스트 등록 → **OS 레벨**(CPU/메모리/프로세스)
6. Spring Boot 2에 JMX 옵션(`-Dcom.sun.management.jmxremote`) 적용 후 Java Gateway 연동, "Generic Java JMX" 템플릿 적용 → **JVM 레벨**(힙/스레드/GC)
7. Spring Boot Actuator 의존성 추가, `/actuator/metrics`·`/actuator/health` 노출, HTTP 에이전트 아이템 구성 → **애플리케이션 레벨**(응답시간/에러율)

### 4-3. 장애 시나리오 및 트리거
8. 임계치 트리거 설계: CPU 80% 초과, 메모리 85% 초과, JVM 힙 초과, HTTP 응답시간 지연, httpd/프로세스 다운 등 3종 이상
9. 트래픽 폭주(로그인/인증 요청 집중) 시나리오를 `ab`/`siege`/`wrk`로 재현 — 콘서트 티켓팅 트래픽 대응 경험과 연결
10. 트리거 강제 발생 및 알림 테스트(SMTP 없으면 Zabbix 액션 로그로 대체 캡처)

### 4-4. 자동화 및 문서화
11. 수동 설치로 원리를 먼저 이해한 뒤, 반복 설치/설정만 Ansible Playbook으로 자동화(쉘 스크립트가 아닌 Ansible — Kevin 교육과정 기준, 레거시 공통 규칙의 예외)
12. **Zabbix 모니터링 가이드 문서**(`docs/zabbix_monitoring_guide.md`) 작성 — 설치 과정, 트리거 설계 근거, 알림 설정 방법, 장애 대응 매뉴얼, 트러블슈팅 포함(약 100페이지 목표)

## 5. 완료 조건 (DoD)
- [ ] VM 3대(웹서버/Spring Boot 2 WAS/Zabbix Server) 정상 기동
- [ ] Zabbix Server/Agent2/Java Gateway/Web UI 정상 기동 및 웹UI 접속 확인
- [ ] 레거시 3개 VM 모두 Zabbix에 호스트로 등록됨
- [ ] OS/JVM/애플리케이션 3단 모니터링 데이터 수집 확인
- [ ] 트리거 3종 이상 설계 및 테스트(강제로 임계치 넘겨서 알림 발생 확인)
- [ ] Ansible Playbook으로 Agent 설치 자동화 완료
- [ ] Zabbix 모니터링 가이드 문서 작성 완료
- [ ] `작업명령어_로그.md`에 전체 설치/설정 과정 기록
- [ ] (전달받는 시점에) Oracle DB 설치 + Zabbix Oracle 모니터링 연동

## 6. 일정
`착수보고서_Zabbix모니터링_이건영.md` 6절(총 19일, 10/8~10/26, 5단계) 참조. 단, 조장님 압축 요청(10/20 마감)과 6일 차이가 있어 아직 공식 조정 전 — Kevin이 조장님과 재협의 필요.

## 7. 의존 관계
- 선행: 없음(VM 3대부터 Kevin이 직접 구축)
- 후행: 없음(레거시 내 완전 독립 작업, 다른 조원 파트와 서버·IP·일정 연동 없음)
- 조건부 의존: Oracle DB는 DB 담당 조원에게서 전달받는 시점에 맞춰 후속 진행(전달 전까지는 1~4단계와 문서화를 먼저 진행)

## 8. 기록 규칙
- `작업명령어_로그.md` → 파트별로 계속 누적 기록("PART 11. legacy-web 웹서버", "PART 12. legacy-was WAS" 등 이미 진행 중)
- 가이드 문서(`docs/zabbix_monitoring_guide.md`)는 작업 로그와 별개 파일로 관리 (작업 로그 = "내가 뭘 했는지" 시간순 원본, 가이드 문서 = "남이 따라할 수 있는 완성형 매뉴얼")
