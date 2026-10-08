# 07. 최신화 — 웹서버(nginx) 설치 + NIC 본딩

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조. 환경: Rocky Linux 10, 작업 PC: 맥북

## 1. 목표
최신화 웹서버 VM에 nginx를 설치하고, 네트워크 인터페이스 본딩(bonding)을 구성한다.

## 2. 배경/컨텍스트
착수보고서 일정상 10/11 작업. 최신화는 레거시와 별도의 VM 3대(웹/앱/DB) 세트로 새로 구축한다. NIC 본딩은 최신화에서 필수 항목.

## 3. 범위
- In-Scope: Rocky Linux 10 VM 생성, nginx 설치/기동, NIC 본딩 구성(2개 이상 인터페이스를 1개 논리 인터페이스로 묶기)
- Out-of-Scope: VIP(Keepalived)는 선택 항목 — 본딩 완료 후 여유 있으면 별도로 진행

## 4. 요구사항
1. 최신화용 VM 3대 생성 계획 수립 (웹: 예 192.168.56.20, 앱: .21, DB: .22 — 레거시와 겹치지 않는 대역)
2. Rocky Linux 10 box로 웹서버 VM 생성 (`vagrant init rockylinux/10` 또는 동일 Vagrantfile에 멀티 VM 블록 추가)
3. VM에 네트워크 인터페이스 2개 추가 (bonding 테스트용)
4. NetworkManager 기반 본딩 구성 (`nmcli con add type bond ...`, `nmcli con add type ethernet ... master bond0`)
5. 본딩 모드 결정 (예: active-backup 또는 802.3ad) — 왜 이 모드를 선택했는지 근거 기록
6. 본딩 상태 확인 (`cat /proc/net/bonding/bond0`)
7. `dnf install nginx -y` → `systemctl enable --now nginx`
8. 방화벽 80(http) 포트 개방
9. 브라우저로 nginx 기본 페이지 접속 확인

## 5. 완료 조건 (DoD)
- [ ] 본딩 인터페이스(bond0) 정상 동작 확인 (`/proc/net/bonding/bond0`에 두 슬레이브 인터페이스 모두 표시)
- [ ] nginx `active (running)` 상태
- [ ] 외부 브라우저에서 nginx 기본 페이지 접속 성공
- [ ] `작업명령어_로그.md`에 본딩 설정 과정과 선택 모드 근거 기록

## 6. 의존 관계
- 선행: 없음 (최신화 환경 신규 시작)
- 후행: PRD 08(앱서버), PRD 10(관리자 CRUD — nginx가 FastAPI로 리버스 프록시)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 8. 최신화 웹서버(nginx) 설치 + NIC 본딩"
- VIP(Keepalived)를 진행하게 되면 같은 PART 안에 하위 섹션으로 추가 기록
