package com.suppleit.backend.service;

import com.suppleit.backend.dto.NoticeDto;
import com.suppleit.backend.mapper.NoticeMapper;
import com.suppleit.backend.mapper.MemberMapper;
import com.suppleit.backend.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final MemberMapper memberMapper;

    // ✅ 모든 공지사항 조회 (Entity → DTO 변환)
    public List<NoticeDto> getAllNotices() {
        return noticeMapper.getAllNotices(); // 이미 NoticeDto 타입이므로 변환 불필요
    }

    // ✅ 특정 공지사항 조회 (Entity → DTO 변환)
    public NoticeDto getNoticeById(Long noticeId) {
        return noticeMapper.getNoticeById(noticeId); // 변환 과정 제거
    }

    // ✅ 공지사항 생성 (관리자만 가능)
    public void createNotice(NoticeDto noticeDto) {
        Notice notice = noticeDto.toEntity();
        noticeMapper.insertNotice(notice);
    }

    // ✅ 공지사항 수정 (관리자만 가능)
    public void updateNotice(Long noticeId, NoticeDto noticeDto) {
        Notice notice = noticeDto.toEntity();
        noticeMapper.updateNotice(noticeId, notice);
    }

    // ✅ 공지사항 삭제 (관리자만 가능)
    public void deleteNotice(Long noticeId) {
        noticeMapper.deleteNotice(noticeId);
    }

    // ✅ 이메일을 통해 member_id 가져오기
    public Long getMemberIdByEmail(String email) {
        return memberMapper.getMemberByEmail(email).getMemberId();
    }
}
