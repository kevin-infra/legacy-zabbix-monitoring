# 09. 최신화 — DB서버(MySQL 8.4) 설치 및 테이블 정식 설계

> 공통 컨텍스트는 `00_마스터_PRD.md` 참조.

## 1. 목표
최신화 DB서버 VM에 MySQL 8.4를 설치하고, `gifticon_users / gifticon_coupons / gifticon_transactions` 3개 테이블을 **정식 설계**(제약조건, 인덱스, FK 포함)로 생성한다.

## 2. 배경/컨텍스트
착수보고서 일정상 10/13 작업. 레거시(PRD 02)는 "간단 버전"이었지만, 최신화는 트랜잭션 정합성(PRD 13)과 장애 시나리오(PRD 14)를 구현해야 하므로 제약조건과 인덱스 설계가 중요하다.

## 3. 범위
- In-Scope: MySQL 8.4 설치, 테이블 3종 정식 설계(FK/인덱스), DB 커넥터 선택
- Out-of-Scope: 애플리케이션 레벨 트랜잭션 로직은 PRD 13에서

## 4. 요구사항
1. MySQL 8.4 설치 (`dnf install mysql-server -y` 또는 MySQL 공식 repo 사용 — Rocky 10 기본 저장소 버전 확인 필요)
2. `systemctl enable --now mysqld`
3. `mysql_secure_installation` 으로 보안 기본값 적용
4. 방화벽 3306 포트 개방
5. DB 커넥터 확정 (SQLAlchemy 권장 — FastAPI와 생태계 호환성이 좋고 ORM으로 트랜잭션 제어가 명시적이라 PRD 13 트랜잭션 구현에 유리. mysql-connector-python은 로우레벨 직접 제어가 필요할 때 대안)
6. 테이블 정식 설계 (예시)
   ```sql
   CREATE TABLE gifticon_users (
     user_id INT AUTO_INCREMENT PRIMARY KEY,
     name VARCHAR(50) NOT NULL,
     phone VARCHAR(20) NOT NULL,
     email VARCHAR(100),
     created_at DATETIME DEFAULT CURRENT_TIMESTAMP
   );
   CREATE TABLE gifticon_coupons (
     coupon_id INT AUTO_INCREMENT PRIMARY KEY,
     coupon_name VARCHAR(100) NOT NULL,
     barcode VARCHAR(50) UNIQUE NOT NULL,
     expiry_date DATE,
     status ENUM('사용전','사용완료','만료') DEFAULT '사용전'
   );
   CREATE TABLE gifticon_transactions (
     transaction_id INT AUTO_INCREMENT PRIMARY KEY,
     user_id INT NOT NULL,
     coupon_id INT NOT NULL,
     issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
     used_at DATETIME NULL,
     FOREIGN KEY (user_id) REFERENCES gifticon_users(user_id),
     FOREIGN KEY (coupon_id) REFERENCES gifticon_coupons(coupon_id),
     INDEX idx_user_id (user_id),
     INDEX idx_coupon_id (coupon_id)
   );
   ```
7. FastAPI(PRD 08) 앱서버에서 SQLAlchemy로 DB 연결 테스트

## 5. 완료 조건 (DoD)
- [ ] MySQL 8.4 `active (running)` 상태
- [ ] 테이블 3종 FK/인덱스 포함 생성 완료 (`SHOW CREATE TABLE`로 확인)
- [ ] FastAPI에서 SQLAlchemy로 커넥션 성공
- [ ] `작업명령어_로그.md`에 설치/스키마 생성 과정 기록

## 6. 의존 관계
- 선행: PRD 08 (커넥터 테스트를 위해 앱서버 필요, 병렬 가능)
- 후행: PRD 10~14 (모든 CRUD/트랜잭션/장애시나리오가 이 스키마 기반)

## 7. 기록 규칙
- `작업명령어_로그.md` → "PART 10. 최신화 DB서버(MySQL 8.4) 설치 및 테이블 설계"
- DB 커넥터 선택 이유를 반드시 기록 (추후 기술문서에서 "왜 SQLAlchemy인가" 질문에 답할 근거)
