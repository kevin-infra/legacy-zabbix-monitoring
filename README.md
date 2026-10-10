# legacy-zabbix-monitoring

레거시 환경(Zabbix 모니터링) 파트 - Kevin 담당 (팀 프로젝트: 서울시 공공데이터 기반 지역추천서비스)

## 구성 파일

- `vagrant/Vagrantfile` - VM 3대(legacy-web / legacy-was / legacy-zabbix) 자동 생성 설정 (VMware Workstation + Vagrant)
- `app/legacy-was/` - Spring Boot 2 최소 앱 소스코드 (legacy-was VM에 배포되는 WAS, Hello World + 부하 테스트용 API)
- `작업명령어_로그.md` - 실습 중 사용한 명령어 전체 기록 (시간순, 파트별)
- `docs/zabbix_monitoring_guide.md` - 설치 매뉴얼 (완성형, 팀 제출용)
- `착수보고서_Zabbix모니터링_이건영.md` - 프로젝트 착수보고서

## 환경 버전

| 항목 | 버전 |
|---|---|
| VMware Workstation Pro | 26.0.0 (build 25388281) |
| Vagrant | 2.4.9 |
| vagrant-vmware-desktop 플러그인 | 3.0.5 |
| Rocky Linux 박스 (generic/rocky8) | 4.3.12 |
| 게스트 OS | Rocky Linux 8.9 (Green Obsidian) |

## VM 구성

| VM 이름 | 역할 | IP | 스펙 |
|---|---|---|---|
| legacy-web | 웹서버(Apache) | 192.168.232.11 | 1GB / 1 vCPU |
| legacy-was | Spring Boot 2 WAS | 192.168.232.12 | 2GB / 2 vCPU |
| legacy-zabbix | Zabbix Server | 192.168.232.13 | 2GB / 2 vCPU |
| legacy-nexus | Nexus Repository (폐쇄망 패키지 관리 실습, KAN-27) | 192.168.232.14 | 2GB / 2 vCPU |

## VM 실행 방법

cd vagrant
vagrant up --provider=vmware_desktop
