# 19. 최신화 — CI/CD (Jenkins) [선택]

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조. 우선순위: 선택 — 일정이 밀리면 제외 가능한 항목(착수보고서 7번 기준)

## 1. 목표
Jenkins 파이프라인을 연동해, 코드 변경 시 Docker 이미지 빌드까지 자동화되는 CI/CD 흐름을 구축한다.

## 2. 배경/컨텍스트
착수보고서 "인프라 목표" 표에 선택 항목으로 명시. Kevin이 별도로 진행 중인 `cicd-jenkins-practice`(Jenkins/Tomcat CI-CD 실습) 경험을 이 프로젝트에도 적용할 수 있는 지점.

## 3. 범위
- In-Scope: Jenkins 설치/연동, 파이프라인 작성(Git push → 테스트 → Docker 빌드)
- Out-of-Scope: 자동 배포(쿠버네티스 Pod까지 자동 배포)는 팀 통합 이후 범위

## 4. 요구사항
1. Jenkins 설치 (별도 VM 또는 컨테이너)
2. GitHub 저장소와 Jenkins 연동 (Webhook 또는 polling)
3. `Jenkinsfile` 작성 — Checkout → (테스트, 있다면) → `docker build` 단계
4. 파이프라인 1회 성공 실행 확인
5. 빌드 결과(이미지 태그)가 PRD 15의 태그 규칙과 일치하는지 확인

## 5. 완료 조건 (DoD)
- [ ] Jenkins 설치 및 GitHub 연동 완료
- [ ] 파이프라인 1회 성공 실행
- [ ] `작업명령어_로그.md`에 기록

## 6. 의존 관계
- 선행: PRD 15 (Docker 이미지 빌드 과정이 먼저 확정되어 있어야 파이프라인화 가능)
- 후행: 없음 (선택 항목)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 20. CI/CD (Jenkins, 선택)"
