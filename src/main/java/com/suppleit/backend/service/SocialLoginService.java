package com.suppleit.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suppleit.backend.constants.MemberRole;
import com.suppleit.backend.constants.SocialType;
import com.suppleit.backend.dto.MemberDto;
import com.suppleit.backend.mapper.MemberMapper;
import com.suppleit.backend.model.Member;
import com.suppleit.backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final MemberMapper memberMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 카카오 로그인
    public Map<String, Object> getKakaoMember(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            log.info("카카오 API 응답: {}", jsonNode.toString());

            // 카카오 API 응답에서 필요한 정보 추출
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            JsonNode profile = kakaoAccount.get("profile");
            
            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            String nickname = profile.has("nickname") ? profile.get("nickname").asText() : "카카오 사용자";

            // 이메일이 없으면 처리 불가
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("카카오 계정에서 이메일을 제공하지 않았습니다. 이메일 제공에 동의해주세요.");
            }

            return processSocialLogin(email, nickname, SocialType.KAKAO);
        } catch (Exception e) {
            log.error("카카오 로그인 처리 중 오류: {}", e.getMessage());
            throw new RuntimeException("카카오 로그인 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 구글 로그인
    public Map<String, Object> getGoogleMember(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            log.info("구글 API 응답: {}", jsonNode.toString());

            String email = jsonNode.has("email") ? jsonNode.get("email").asText() : null;
            String nickname = jsonNode.has("name") ? jsonNode.get("name").asText() : "구글 사용자";

            // 이메일이 없으면 처리 불가
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("구글 계정에서 이메일을 제공하지 않았습니다. 이메일 제공에 동의해주세요.");
            }

            return processSocialLogin(email, nickname, SocialType.GOOGLE);
        } catch (Exception e) {
            log.error("구글 로그인 처리 중 오류: {}", e.getMessage());
            throw new RuntimeException("구글 로그인 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 네이버 로그인
    public Map<String, Object> getNaverMember(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            log.info("네이버 API 응답: {}", jsonNode.toString());

            // 네이버 API는 response 객체 안에 실제 데이터가 있음
            JsonNode responseData = jsonNode.get("response");
            
            String email = responseData.has("email") ? responseData.get("email").asText() : null;
            String nickname = responseData.has("nickname") ? responseData.get("nickname").asText() : 
                    responseData.has("name") ? responseData.get("name").asText() : "네이버 사용자";

            // 이메일이 없으면 처리 불가
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("네이버 계정에서 이메일을 제공하지 않았습니다. 이메일 제공에 동의해주세요.");
            }

            return processSocialLogin(email, nickname, SocialType.NAVER);
        } catch (Exception e) {
            log.error("네이버 로그인 처리 중 오류: {}", e.getMessage());
            throw new RuntimeException("네이버 로그인 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 소셜 로그인 공통 처리 메서드
    private Map<String, Object> processSocialLogin(String email, String nickname, SocialType socialType) {
        Member existingMember = memberMapper.getMemberByEmail(email);
        String jwtToken;
        String refreshToken;
        MemberDto memberDto;

        if (existingMember != null) {
            // 이미 등록된 회원인 경우
            log.info("기존 {} 계정으로 로그인: {}", socialType, email);
            
            // 소셜 타입이 다른 경우 오류 발생
            if (existingMember.getSocialType() != socialType && existingMember.getSocialType() != SocialType.NONE) {
                throw new IllegalArgumentException("이미 다른 소셜 계정(" + existingMember.getSocialType() + ")으로 가입된 이메일입니다.");
            }
            
            String role = existingMember.getMemberRole().name();
            jwtToken = jwtTokenProvider.createToken(email, role);
            refreshToken = jwtTokenProvider.createRefreshToken(email);
            memberDto = MemberDto.fromEntity(existingMember);
        } else {
            // 신규 회원 등록
            // 소셜 로그인 이용자는 랜덤 패스워드 부여 (로컬 로그인 불가)
            String randomPassword = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(randomPassword);
            
            Member newMember = Member.builder()
                    .email(email)
                    .password(encodedPassword) // 암호화된 랜덤 비밀번호
                    .nickname(nickname)
                    .memberRole(MemberRole.USER) // 기본 사용자 권한
                    .socialType(socialType)
                    .build();

            memberMapper.insertMember(newMember);
            log.info("{} 계정 신규 등록: {}", socialType, email);
            
            // 방금 등록한 회원 정보 조회
            Member savedMember = memberMapper.getMemberByEmail(email);
            
            jwtToken = jwtTokenProvider.createToken(email, MemberRole.USER.name());
            refreshToken = jwtTokenProvider.createRefreshToken(email);
            memberDto = MemberDto.fromEntity(savedMember);
        }

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", jwtToken);
        response.put("refreshToken", refreshToken);
        response.put("member", memberDto);

        return response;
    }
}