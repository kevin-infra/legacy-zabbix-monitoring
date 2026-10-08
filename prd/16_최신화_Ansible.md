# 16. 최신화 — Ansible 자동화(Playbook)

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조.

## 1. 목표
최신화 웹/앱/DB 서버 설치 과정(PRD 07, 08, 09에서 수동으로 진행했던 작업)을 Ansible Playbook으로 자동화한다.

## 2. 배경/컨텍스트
착수보고서 일정상 10/23(작성)~10/24(반복 실행 테스트 및 보완). 레거시의 쉘 스크립트 자동화(PRD 06)와 대비되는 "최신화식 자동화" 사례 — 두 방식의 차이(멱등성, 선언적 구성 등)를 보여줄 수 있는 포트폴리오 포인트.

## 3. 범위
- In-Scope: Ansible 설치, inventory 구성, 웹/앱/DB 각 Playbook 작성, 반복 실행 테스트
- Out-of-Scope: 애플리케이션 코드 배포 자동화는 선택 범위 (CI/CD, PRD 19와 연계 가능)

## 4. 요구사항
1. Ansible 컨트롤 노드 결정 (맥북 또는 별도 VM)
2. `inventory.ini` 작성 — 웹/앱/DB 3대 VM의 IP와 그룹(`[web]`, `[app]`, `[db]`) 정의
3. SSH 키 기반 접속 설정 (패스워드 없이 Ansible이 각 VM에 접속 가능하도록)
4. `playbook_web.yml` — nginx 설치 + 기동 + 방화벽 개방 (PRD 07 내용을 선언적으로 재현)
5. `playbook_app.yml` — Python/FastAPI 환경 구성 + 앱 배포 (PRD 08 내용 재현)
6. `playbook_db.yml` — MySQL 8.4 설치 + 테이블 생성까지 포함 (PRD 09 내용 재현, 테이블 생성 SQL은 `mysql_db`/`mysql_query` 모듈 또는 스크립트 파일로 처리)
7. `ansible-playbook -i inventory.ini site.yml` 로 전체 실행
8. **멱등성 테스트**: 같은 playbook을 2번 연속 실행해도 에러 없이 "변경 없음" 상태로 끝나는지 확인 (Ansible의 핵심 특징)

## 5. 완료 조건 (DoD)
- [ ] 3개 Playbook 작성 완료
- [ ] 전체 Playbook 1회 실행으로 수동 설치와 동일한 결과 재현
- [ ] 동일 Playbook 재실행 시 멱등성 확인 (changed=0)
- [ ] `작업명령어_로그.md`에 Playbook 구조 및 테스트 결과 기록

## 6. 의존 관계
- 선행: PRD 07, 08, 09 (수동 설치 과정이 먼저 검증되어 있어야 Playbook으로 옮길 수 있음)
- 후행: PRD 17 (최종 점검에서 이 자동화 결과까지 포함해 검증)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 17. Ansible 자동화(Playbook)"
- 멱등성 테스트 결과(1차 실행 changed 수, 2차 실행 changed 수)를 숫자로 명확히 기록
