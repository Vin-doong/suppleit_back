package com.suppleit.backend.service;

import com.suppleit.backend.constants.MemberRole;
import com.suppleit.backend.constants.SocialType;
import com.suppleit.backend.dto.MemberDto;
import com.suppleit.backend.mapper.MemberMapper;
import com.suppleit.backend.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public void insertMember(MemberDto memberDto) {
        if (!checkEmail(memberDto.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        MemberRole memberRole = memberDto.getMemberRole() != null ? memberDto.getMemberRole() : MemberRole.USER;
        SocialType socialType = memberDto.getSocialType() != null ? memberDto.getSocialType() : SocialType.NONE;

        // 비밀번호 유효성 검사 및 암호화
        if (socialType == SocialType.NONE) {
            validatePassword(memberDto.getPassword());
            String encodedPassword = passwordEncoder.encode(memberDto.getPassword());
            memberDto.setPassword(encodedPassword);
        } else {
            memberDto.setPassword("SOCIAL_LOGIN_USER");
        }
        
        // 추가해야 할 코드: Member 객체 생성 및 DB 저장
        Member member = Member.builder()
                .email(memberDto.getEmail())
                .password(memberDto.getPassword())
                .nickname(memberDto.getNickname())
                .gender(memberDto.getGender())
                .birth(memberDto.getBirth())
                .memberRole(memberRole)
                .socialType(socialType)
                .build();
        
        // 실제 DB에 저장하는 호출
        memberMapper.insertMember(member);
        
        System.out.println("회원 저장 완료: " + member.getEmail());
    }
    
    // 비밀번호 검증
    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("비밀번호는 숫자를 포함해야 합니다.");
        }
        
        if (!password.matches(".*[!@#$%^&*()\\-_=+\\\\|\\[{\\]};:'\",<.>/?].*")) {
            throw new IllegalArgumentException("비밀번호는 특수문자를 포함해야 합니다.");
        }
    }

    // 이메일 중복 검사
    public boolean checkEmail(String email) {
        return memberMapper.checkEmail(email) == 0;
    }

    // 닉네임 중복 검사
    public boolean checkNickname(String nickname) {
        return memberMapper.checkNickname(nickname.toLowerCase()) == 0;
    }

    // 닉네임 유효성 검사 및 중복 검사
    public boolean validateAndCheckNickname(String nickname) {
        nickname = nickname.trim();
        if (nickname.length() < 3 || nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 3~20자 사이여야 합니다.");
        }
    
        if (!nickname.matches("^[a-zA-Z0-9가-힣]+$")) {
            throw new IllegalArgumentException("닉네임은 영문, 숫자, 한글만 사용할 수 있습니다.");
        }
    
        return checkNickname(nickname.toLowerCase());
    }

    // 이메일로 회원 조회
    public Optional<MemberDto> getMemberByEmail(String email) {
        return Optional.ofNullable(memberMapper.getMemberByEmail(email))
                .map(MemberDto::fromEntity);  // 수정된 fromEntity 메서드 사용
    }
    
    
    // 회원 정보 수정
    public void updateMemberInfo(String email, MemberDto memberDto) {
        Member existingMember = memberMapper.getMemberByEmail(email);
        
        if (existingMember == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        
        // 닉네임 변경 시 중복 검사
        if (memberDto.getNickname() != null && !existingMember.getNickname().equals(memberDto.getNickname()) 
                && !checkNickname(memberDto.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        
        // 업데이트할 필드 설정
        if (memberDto.getNickname() != null) existingMember.setNickname(memberDto.getNickname());
        if (memberDto.getGender() != null) existingMember.setGender(memberDto.getGender());
        if (memberDto.getBirth() != null) existingMember.setBirth(memberDto.getBirth());
        
        memberMapper.updateMemberInfo(existingMember);
    }

    // 회원 탈퇴
    public void deleteMemberByEmail(String email) {
        Member member = memberMapper.getMemberByEmail(email);
    
        if (member == null) {
            throw new IllegalArgumentException("해당 이메일로 가입된 사용자가 없습니다.");
        }

        memberMapper.deleteMemberByEmail(email);
    }
    
    // 이메일로 회원 역할 조회
    public String getMemberRoleByEmail(String email) {
        Member member = memberMapper.getMemberByEmail(email);
        if (member == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return member.getMemberRole().name();
    }
}