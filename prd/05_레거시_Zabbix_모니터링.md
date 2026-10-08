# 05. 레거시 — Zabbix 모니터링 구축 + 가이드 문서화

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조.
> ⚠️ **날짜 미확정**: 착수보고서상 10/6 조장님 재확인으로 Zabbix가 최신화→레거시로 이동했지만, 일별 일정(6번 섹션)은 아직 반영 전입니다. 이 PRD를 시작하기 전에 Kevin과 "레거시 4일(10/7~10/10) 안에 끼워넣을지, 별도 날짜를 추가할지"를 먼저 확정해야 합니다.

## 1. 목표
레거시 환경에 Zabbix Server/Agent를 설치하고, 임계치 기반 트리거를 설계해 장애 발생 시 알림이 오도록 구성한다. 추가로 팀/본인이 참고할 **Zabbix 모니터링 가이드 문서**를 작성한다.

## 2. 배경/컨텍스트
Kevin의 실무 경력(JENNIFER, ZABBIX, TOBIT 등 모니터링 툴 운영 경험)과 직접 연결되는 영역. 착수보고서 "인프라 목표" 표에 "Zabbix Agent 설치, 임계치 기반 트리거 설계, 장애 발생 시 알림, Zabbix 모니터링 가이드 문서 작성"이 필수 항목으로 명시됨.

## 3. 범위
- In-Scope: Zabbix Server + Agent 설치, 기본 호스트 등록, 임계치 트리거(CPU/메모리/디스크/서비스 다운 등) 설계, 알림 테스트, 가이드 문서 작성
- Out-of-Scope: 최신화 환경 모니터링(LGTM 스택, 정현목님 담당 — Kevin 범위 아님)

## 4. 요구사항
1. Zabbix Server용 VM 결정 (레거시 웹/앱/DB 중 하나에 함께 설치할지, 별도 VM을 둘지 결정 — 리소스 상황에 따라)
2. Zabbix 공식 저장소 추가 → `dnf install zabbix-server-mysql zabbix-web-mysql zabbix-agent -y` 등
3. Zabbix DB 스키마 생성 및 Zabbix Server용 DB 계정 설정 (기존 `gifticon_legacy` DB와는 별도 DB 사용 권장)
4. Zabbix Server, Zabbix Agent, httpd(Zabbix 웹UI용), php-fpm 등 관련 서비스 기동
5. 웹/앱/DB 각 VM에 Zabbix Agent 설치 및 Zabbix Server에 호스트로 등록
6. 임계치 트리거 설계 (예시)
   - CPU 사용률 80% 초과
   - 메모리 사용률 85% 초과
   - httpd/mysqld 서비스 다운
   - (가능하다면) MySQL 커넥션 수 임계치 — PRD 14 장애 시나리오와 연계
7. 알림 테스트 (이메일 또는 Zabbix 자체 알림 로그로 확인 — SMTP 서버가 없으면 Zabbix 내부 액션 로그 캡처로 대체)
8. **Zabbix 모니터링 가이드 문서** 작성 (별도 문서, 예: `docs/zabbix_monitoring_guide.md`) — 설치 과정, 트리거 설계 근거, 알림 설정 방법, 장애 대응 매뉴얼 포함

## 5. 완료 조건 (DoD)
- [ ] Zabbix Server/Agent 정상 기동 및 웹UI 접속 확인
- [ ] 레거시 3개 VM 모두 Zabbix에 호스트로 등록됨
- [ ] 트리거 3종 이상 설계 및 테스트(강제로 임계치 넘겨서 알림 발생 확인)
- [ ] Zabbix 모니터링 가이드 문서 작성 완료
- [ ] `작업명령어_로그.md`에 전체 설치/설정 과정 기록

## 6. 의존 관계
- 선행: PRD 01, 02 (모니터링 대상 서버들이 먼저 존재해야 함)
- 후행: 없음 (레거시 내 독립 작업, 최신화와는 무관)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 6. Zabbix 모니터링 구축"
- 가이드 문서는 작업 로그와 별개 파일로 관리 (작업 로그 = "내가 뭘 했는지", 가이드 문서 = "남이 따라할 수 있는 매뉴얼")
