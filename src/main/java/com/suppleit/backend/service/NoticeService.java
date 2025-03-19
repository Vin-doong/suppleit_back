package com.suppleit.backend.service;

import com.suppleit.backend.dto.NoticeDto;
import com.suppleit.backend.mapper.NoticeMapper;
import com.suppleit.backend.mapper.MemberMapper;
import com.suppleit.backend.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final MemberMapper memberMapper;
    private final FileService fileService;

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
    @Transactional
    public void createNotice(NoticeDto noticeDto, MultipartFile image, MultipartFile attachment) throws IOException {
        // 이미지 처리
        if (image != null && !image.isEmpty()) {
            String imagePath = fileService.saveImage(image);
            noticeDto.setImagePath(imagePath);
        }
        
        // 첨부파일 처리
        if (attachment != null && !attachment.isEmpty()) {
            String attachmentPath = fileService.saveAttachment(attachment);
            noticeDto.setAttachmentPath(attachmentPath);
            noticeDto.setAttachmentName(attachment.getOriginalFilename());
        }
        
        Notice notice = noticeDto.toEntity();
        noticeMapper.insertNotice(notice);
    }

    // 공지사항 수정 (이미지 및 첨부파일 포함)
    @Transactional
    public void updateNotice(Long noticeId, NoticeDto noticeDto, MultipartFile image, MultipartFile attachment) throws IOException {
        // 기존 공지사항 조회
        NoticeDto existingNotice = noticeMapper.getNoticeById(noticeId);
        
        if (existingNotice == null) {
            throw new IllegalArgumentException("해당 공지사항을 찾을 수 없습니다: " + noticeId);
        }
        
        // 이미지 처리
        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제
            if (existingNotice.getImagePath() != null) {
                fileService.deleteImage(existingNotice.getImagePath());
            }
            // 새 이미지 저장
            String imagePath = fileService.saveImage(image);
            noticeDto.setImagePath(imagePath);
        } else {
            // 이미지 변경 없으면 기존 이미지 경로 유지
            noticeDto.setImagePath(existingNotice.getImagePath());
        }
        
        // 첨부파일 처리
        if (attachment != null && !attachment.isEmpty()) {
            // 기존 첨부파일 삭제
            if (existingNotice.getAttachmentPath() != null) {
                fileService.deleteAttachment(existingNotice.getAttachmentPath());
            }
            // 새 첨부파일 저장
            String attachmentPath = fileService.saveAttachment(attachment);
            noticeDto.setAttachmentPath(attachmentPath);
            noticeDto.setAttachmentName(attachment.getOriginalFilename());
        } else {
            // 첨부파일 변경 없으면 기존 정보 유지
            noticeDto.setAttachmentPath(existingNotice.getAttachmentPath());
            noticeDto.setAttachmentName(existingNotice.getAttachmentName());
        }
        
        Notice notice = noticeDto.toEntity();
        noticeMapper.updateNotice(noticeId, notice);
    }


    // ✅ 공지사항 삭제 (관리자만 가능)
    @Transactional
    public void deleteNotice(Long noticeId) throws IOException {
        // 이미지 및 첨부파일 삭제를 위해 공지사항 조회
        NoticeDto notice = noticeMapper.getNoticeById(noticeId);
        
        if (notice != null) {
            // 이미지 삭제
            if (notice.getImagePath() != null) {
                fileService.deleteImage(notice.getImagePath());
            }
            
            // 첨부파일 삭제
            if (notice.getAttachmentPath() != null) {
                fileService.deleteAttachment(notice.getAttachmentPath());
            }
        }
        
        noticeMapper.deleteNotice(noticeId);
    }

    // ✅ 이메일을 통해 member_id 가져오기
    public Long getMemberIdByEmail(String email) {
        return memberMapper.getMemberByEmail(email).getMemberId();
    }
}
