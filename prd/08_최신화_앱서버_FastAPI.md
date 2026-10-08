# 08. 최신화 — 앱서버(FastAPI) 환경 구성

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조.

## 1. 목표
최신화 앱서버 VM에 Python + FastAPI + uvicorn 환경을 구성하고, 앱의 기본 뼈대(디렉토리 구조, 헬스체크 엔드포인트)를 작성한다.

## 2. 배경/컨텍스트
착수보고서 일정상 10/12 작업. 백엔드 언어가 Node.js(레거시) → FastAPI/Python(최신화)로 이원화되므로, 조장님 FastAPI 코칭을 활용할 것(착수보고서 7번 리스크 대응 참고).

## 3. 범위
- In-Scope: Python 가상환경, FastAPI/uvicorn 설치, 프로젝트 기본 구조, 헬스체크 API
- Out-of-Scope: 실제 비즈니스 로직(CRUD)은 PRD 10~12에서

## 4. 요구사항
1. Python 3 설치 확인 (`python3 --version`) — Rocky Linux 10 기본 버전 확인
2. `python3 -m venv venv` 로 가상환경 생성
3. `source venv/bin/activate` 로 가상환경 활성화
4. `pip install fastapi uvicorn[standard]` 설치
5. 프로젝트 디렉토리 구조 설계 (예시)
   ```
   app/
     main.py
     routers/
     models/
     db.py
   ```
6. `main.py`에 헬스체크 엔드포인트 작성: `GET /health` → `{"status": "ok"}` 반환
7. `uvicorn main:app --host 0.0.0.0 --port 8000` 으로 기동
8. 방화벽 8000번 포트 개방 (내부 통신용이면 nginx 리버스프록시 경유 설계도 함께 고려)
9. nginx(PRD 07)에서 FastAPI(8000)로 리버스 프록시 연결 설정 (`proxy_pass http://127.0.0.1:8000;` 등)

## 5. 완료 조건 (DoD)
- [ ] FastAPI 앱이 uvicorn으로 정상 기동
- [ ] `/health` 엔드포인트 응답 확인 (`curl http://localhost:8000/health`)
- [ ] nginx → FastAPI 리버스 프록시 연결 확인 (`curl http://192.168.56.20/health`)
- [ ] `작업명령어_로그.md`에 가상환경/패키지 설치 과정 기록

## 6. 의존 관계
- 선행: PRD 07 (nginx와 연결하기 위해 웹서버가 먼저 존재해야 함, 또는 병렬 진행 후 연결)
- 후행: PRD 09(DB), PRD 10~12(CRUD 로직)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 9. 최신화 앱서버(FastAPI) 환경 구성"
- requirements.txt로 패키지 버전 고정하고, 그 파일 경로를 로그에 명시
