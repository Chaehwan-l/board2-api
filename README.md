# 주요 기능
- 게시판 핵심 도메인 : 게시글 작성, 조회, 수정, 삭제, 파일 첨부 및 계층형 댓글
- Spring Security 인증/인가 : 자체 로그인과 OAuth2 로그인을 통합 지원 (Stateless 보안 아키텍처)

# 핵심 비즈니스 로직
### 1. 팬텀 토큰(Phantom Token) 인증 아키텍처
클라이언트(브라우저)에게는 내부 정보가 담긴 JWT를 직접 노출하지 않고, 의미를 알 수 없는 UUID(팬텀 토큰)만 쿠키나 헤더를 통해 발급
1. 발급: 로그인 시 서버는 내부용 JWT를 생성하고, 이를 Redis에 UUID : JWT 형태로 저장한 뒤 클라이언트에 UUID만 반환
2. 검증: API 요청 시 클라이언트가 UUID를 보내면, Spring Security 필터(PhantomTokenFilter)가 Redis에서 매칭되는 JWT를 꺼내와 서명을 검증하고 SecurityContext에 인가 정보를 주입
3. 효과: 클라이언트 측 JWT 탈취로 인한 피해를 방지하고, 토큰 강제 만료(로그아웃, 기기 제어)를 Redis를 통해 즉각적이고 확실하게 처리 가능

### 2. Redis를 활용한 대용량 트래픽 처리 (Cache & Write-Back)
관계형 데이터베이스(DB)의 부하를 최소화하기 위해 Redis를 활용
- Cache-Aside (유저 프로필 조회): 게시글이나 댓글 작성자의 닉네임 등 변경이 잦지 않은 정보는 DB 조회 전 Redis를 선조회하여 응답 속도 향상
- Write-Back (게시글 조회수): 조회수가 발생할 때마다 DB에 UPDATE 쿼리를 날리지 않고 Redis에 조회수를 누적시킨 후, 백그라운드 스케줄러가 주기적으로 Redis의 데이터를 DB에 벌크 업데이트(Bulk Update)

# Infra/DevOps
### 1. CI/CD 파이프라인 (GitHub Actions)
- 도커 레이어 캐싱을 극대화하기 위해 gradlew, build.gradle, settings.gradle 등 설정 파일을 먼저 복사한 후 빌드
- Amazon ECR Push: OIDC 자격 증명을 통해 Amazon ECR에 로그인 후, 빌드된 이미지를 푸시
- EC2 인스턴스에 SSH로 직접 접근하지 않고, 배포 전용 IAM Role을 부여받아 AWS Systems Manager (SSM) Send-Command를 호출

### 2. 시스템 아키텍처
- 프론트엔드(Google AI Studio) : Vercel 
- 백엔드 : AWS EC2
- 캐시/세션 : Redis
- DBMS : AWS RDS
- 파일 처리 : AWS S3 Bucket
- Docker image : AWS ECR
- CI/CD : GitHub Actions, AWS ECR, AWS SSM
