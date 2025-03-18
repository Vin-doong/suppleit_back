# 서플잇(SUPPLE IT) 권한 관리 시스템 개선 문서

**작성일**: 2025년 3월 17일  
**작성자**: 백엔드 개발팀  
**문서 목적**: 현재 권한 관리 시스템 분석 및 개선 방안 제시

## 1. 현재 시스템 분석

### 1.1 현재 구현된 권한 관리 개요

현재 서플잇 프로젝트는 Spring Security와 JWT를 기반으로 권한 관리를 구현하고 있습니다. 사용자는 크게 'USER'와 'ADMIN' 두 가지 역할로 구분되며, 각 역할에 따라 접근 가능한 API가 제한됩니다.

### 1.2 주요 구성 요소

- **MemberRole Enum**: 사용자 역할 정의 (USER, ADMIN)
- **JwtTokenProvider**: 토큰 생성 및 검증, 사용자 역할 포함
- **SecurityConfig**: URL 패턴별 접근 권한 설정
- **JwtFilter**: 요청마다 토큰 검증 및 인증 정보 설정
- **JwtTokenBlacklistService**: 로그아웃된 토큰 무효화 관리

### 1.3 현재 권한별 접근 제어

| 역할 | 접근 가능 리소스 |
|------|-----------------|
| 모든 사용자 | 회원가입, 로그인, 공지사항 조회 등 |
| 인증된 사용자 (USER, ADMIN) | 회원 정보 조회, 비밀번호 변경, 회원 탈퇴 등 |
| 관리자 (ADMIN) | 공지사항 생성/수정/삭제, 관리자 전용 API 등 |

## 2. 개선이 필요한 부분

### 2.1 관리자 계정 관리 개선

#### 현재 문제점:
- 관리자 계정 생성 메커니즘이 명시적으로 구현되어 있지 않음
- 기존 관리자가 다른 사용자에게 관리자 권한을 부여하는 기능 부재
- 최초 관리자 설정 방법이 정의되지 않음

#### 개선 방안:
1. 초기 관리자 설정 메커니즘 구현 (시스템 초기화 시)
2. 관리자 권한 부여/취소 API 개발
3. 관리자 권한 요청 및 승인 워크플로우 구현

### 2.2 토큰 관리 및 보안 강화

#### 현재 문제점:
- 토큰 갱신(refresh) 메커니즘 부재 - 만료 시 반드시 재로그인 필요
- 블랙리스트가 메모리에만 저장되어 서버 재시작 시 정보 소실
- 토큰 유효 기간이 고정되어 있음 (86400000 밀리초 = 1일)

#### 개선 방안:
1. 리프레시 토큰 시스템 구현
2. Redis 또는 DB를 활용한 영구적인 토큰 블랙리스트 저장소 구현
3. 토큰 유효 기간을 환경/사용 상황에 따라 설정 가능하도록 개선

### 2.3 역할 및 권한 모델 확장

#### 현재 문제점:
- USER와 ADMIN 두 가지 역할만 존재하여 세분화된 권한 제어 어려움
- 역할 기반 접근 제어(RBAC)만 구현되어 있어 유연성 부족
- 권한 변경 이력 관리 기능 없음

#### 개선 방안:
1. 세분화된 권한 모델 도입 (예: 상품 관리자, 게시판 관리자 등)
2. 권한과 역할을 분리하여 더 유연한 접근 제어 구현
3. 권한 변경 이력을 기록하고 추적할 수 있는 로깅 시스템 구현

### 2.4 API 접근 관리 강화

#### 현재 문제점:
- URL 패턴 기반 권한 설정만 구현되어 있어 세밀한 제어 어려움
- 일부 관리자 기능만 권한 제어가 구현됨
- 클라이언트 IP 기반 접근 제어 등 추가 보안 기능 미비

#### 개선 방안:
1. 메서드 수준의 접근 제어 구현 (@PreAuthorize 등 활용)
2. 세부적인 리소스 접근 권한 체계 도입
3. IP 기반 필터링, 비정상 접근 탐지 등 추가 보안 강화

## 3. 구체적인 구현 방안

### 3.1 관리자 계정 관리 API

```java
// 관리자 권한 부여 API
@PostMapping("/admin/grant-role")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> grantAdminRole(@RequestParam String email, @RequestParam String role, HttpServletRequest request) {
    // 권한 부여 로직 구현
    String adminEmail = jwtTokenProvider.getEmail(parseBearerToken(request));
    memberService.changeRole(email, MemberRole.fromString(role), adminEmail);
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", String.format("사용자 %s에게 %s 역할이 부여되었습니다.", email, role)
    ));
}
```

### 3.2 토큰 리프레시 시스템

```java
@PostMapping("/auth/refresh")
public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
    String refreshToken = request.get("refreshToken");
    
    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "유효하지 않은 리프레시 토큰입니다."));
    }
    
    String email = jwtTokenProvider.getEmail(refreshToken);
    String role = jwtTokenProvider.getRole(refreshToken);
    
    String newAccessToken = jwtTokenProvider.createToken(email, role);
    
    return ResponseEntity.ok(Map.of(
        "accessToken", newAccessToken
    ));
}
```

### 3.3 영구적인 토큰 블랙리스트 저장소

```java
@Service
public class JwtTokenBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public JwtTokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    // 토큰을 블랙리스트에 추가
    public void addToBlacklist(String token, Long expiryTimeInMillis) {
        long ttl = expiryTimeInMillis - System.currentTimeMillis();
        redisTemplate.opsForValue().set("blacklist:" + token, true, ttl, TimeUnit.MILLISECONDS);
    }
    
    // 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get("blacklist:" + token));
    }
}
```

### 3.4 세분화된 권한 모델

```java
// 권한 정의
public enum Permission {
    READ_PRODUCT,
    WRITE_PRODUCT,
    DELETE_PRODUCT,
    READ_NOTICE,
    WRITE_NOTICE,
    DELETE_NOTICE,
    MANAGE_USERS,
    MANAGE_ROLES
}

// 역할-권한 매핑 테이블
CREATE TABLE `Role_Permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `role_name` VARCHAR(20) NOT NULL,
  `permission` VARCHAR(50) NOT NULL,
  UNIQUE KEY (`role_name`, `permission`)
);

// 권한 변경 이력 테이블
CREATE TABLE `Role_Change_Log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `member_id` BIGINT NOT NULL,
  `admin_id` BIGINT NOT NULL,
  `old_role` VARCHAR(20) NOT NULL,
  `new_role` VARCHAR(20) NOT NULL,
  `change_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`member_id`) REFERENCES `Member`(`member_id`),
  FOREIGN KEY (`admin_id`) REFERENCES `Member`(`member_id`)
);
```

## 4. 구현 우선순위 및 로드맵

### 4.1 1단계 (우선 구현)
- 초기 관리자 계정 설정 메커니즘
- 토큰 블랙리스트의 Redis/DB 저장소 전환
- 관리자 권한 부여/취소 API

### 4.2 2단계 (중기 구현)
- 토큰 리프레시 시스템 구현
- 세분화된 URL 패턴 권한 설정
- 권한 변경 로깅 시스템

### 4.3 3단계 (장기 구현)
- 세분화된 권한 모델 도입
- 메서드 수준 접근 제어 구현
- 고급 보안 기능 (IP 기반 필터링, 비정상 접근 탐지 등)

## 5. 결론 및 제언

현재 서플잇 프로젝트의 권한 관리 시스템은 기본적인 기능을 충실히 구현하고 있으나, 사용자 기반 확대와 서비스 복잡성 증가에 대비하여 위에 제시된 개선사항을 단계적으로 적용할 필요가 있습니다.

특히 초기 관리자 설정 메커니즘과 토큰 관리 개선은 시스템 안정성과 보안을 위해 우선적으로 구현되어야 합니다. 장기적으로는 세분화된 권한 모델을 통해 더 유연하고 확장 가능한 시스템으로 발전시키는 것이 바람직합니다.

---

**참고자료**:
- Spring Security 공식 문서: https://docs.spring.io/spring-security/reference/
- JWT 공식 사이트: https://jwt.io/
- Redis 공식 문서: https://redis.io/documentation
