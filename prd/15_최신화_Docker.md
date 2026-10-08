# 15. 최신화 — Docker 이미지 빌드 및 컨테이너 검증

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조. ★ 이 단계 완료 전까지 쿠버네티스 관련 작업 착수 금지 (착수보고서 5번 작업원칙)

## 1. 목표
최신화 앱(FastAPI)을 Docker 이미지로 빌드하고, 컨테이너 환경에서 전체 플로우(웹→앱→DB)가 정상 동작하는지 검증한다.

## 2. 배경/컨텍스트
착수보고서 일정상 10/21(빌드)~10/22(검증). 추후 팀 공통 쿠버네티스 클러스터에 Pod로 배포될 예정이므로, 이미지 태그에 `gifticon_` 접두사 규칙을 미리 적용해 통합 지연에 대비.

## 3. 범위
- In-Scope: Dockerfile 작성, 이미지 빌드, 컨테이너 기동, 컨테이너 환경에서 end-to-end 재검증
- Out-of-Scope: 쿠버네티스/Pod 배포 (팀 통합 단계, 이 PRD 범위 밖)

## 4. 요구사항
1. `Dockerfile` 작성 (FastAPI 앱 기준)
   ```dockerfile
   FROM python:3.11-slim
   WORKDIR /app
   COPY requirements.txt .
   RUN pip install --no-cache-dir -r requirements.txt
   COPY . .
   CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
   ```
2. 이미지 태그 규칙 적용: `gifticon-app:v1` 형태 (팀 공통 규칙이 공유되면 그에 맞춰 조정 — 00_마스터_PRD.md 6-1 참고)
3. `docker build -t gifticon-app:v1 .`
4. DB 연결 정보를 환경변수로 분리 (`.env` 또는 `docker run -e`)
5. `docker run -d -p 8000:8000 --env-file .env gifticon-app:v1`
6. 컨테이너 안에서 앱이 외부 DB(MySQL 8.4 VM)에 정상 연결되는지 확인
7. PRD 10~14의 핵심 기능(등록/발급/조회/사용/트랜잭션)을 컨테이너 환경에서 다시 한 번 end-to-end 테스트

## 5. 완료 조건 (DoD)
- [ ] Docker 이미지 빌드 성공
- [ ] 컨테이너 기동 및 DB 연결 성공
- [ ] 컨테이너 환경에서 핵심 기능 전체 재검증 통과
- [ ] `작업명령어_로그.md`에 Dockerfile 내용 및 빌드/검증 과정 기록

## 6. 의존 관계
- 선행: PRD 08~14 (앱 로직이 완성되어 있어야 이미지화 가능)
- 후행: PRD 16(Ansible), 팀 통합(쿠버네티스 Pod 배포 — Kevin 범위 밖일 수 있음, 확인 필요)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 16. Docker 이미지 빌드 및 컨테이너 검증"
- Dockerfile 전체 내용과 이미지 태그 규칙을 명확히 기록 (팀 통합 시 바로 참고 가능하도록)
