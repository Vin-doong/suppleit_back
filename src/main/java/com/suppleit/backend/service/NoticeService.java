package com.suppleit.backend.service;

import com.suppleit.backend.dto.NoticeDto;
import com.suppleit.backend.mapper.NoticeMapper;
import com.suppleit.backend.mapper.MemberMapper;
import com.suppleit.backend.model.Notice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final MemberMapper memberMapper;
    private final FileService fileService;

    // ✅ 모든 공지사항 조회 (Entity → DTO 변환)
    public List<NoticeDto> getAllNotices() {
        return noticeMapper.getAllNotices(); // 이미 NoticeDto 타입이므로 변환 불필요
    }

    // ✅ 특정 공지사항 조회 (Entity → DTO 변환)
    @Transactional
    public NoticeDto getNoticeById(Long noticeId) {
        // 조회수 증가 로직 추가
        noticeMapper.incrementViews(noticeId);
        return noticeMapper.getNoticeById(noticeId);
    }

    // ✅ 공지사항 생성 (관리자만 가능)
    @Transactional
    public void createNotice(NoticeDto noticeDto, MultipartFile image, MultipartFile attachment) throws IOException {
        log.info("공지사항 생성 시작 - 이미지: {}, 첨부파일: {}", 
            image != null ? image.getOriginalFilename() : "없음", 
            attachment != null ? attachment.getOriginalFilename() : "없음");
            
        // 이미지 처리
        if (image != null && !image.isEmpty()) {
            String imagePath = fileService.saveImage(image);
            noticeDto.setImagePath(imagePath);
            log.info("이미지 저장 완료: {}", imagePath);
        }
        
        // 첨부파일 처리
        if (attachment != null && !attachment.isEmpty()) {
            String attachmentPath = fileService.saveAttachment(attachment);
            noticeDto.setAttachmentPath(attachmentPath);
            noticeDto.setAttachmentName(attachment.getOriginalFilename());
            log.info("첨부파일 저장 완료: {}, 파일명: {}", attachmentPath, attachment.getOriginalFilename());
        }
        
        Notice notice = noticeDto.toEntity();
        noticeMapper.insertNotice(notice);
        
        // noticeId를 DTO에 설정 (반환값이 필요한 경우)
        noticeDto.setNoticeId(notice.getNoticeId());
        log.info("공지사항 생성 완료, ID: {}", notice.getNoticeId());
    }

    // 공지사항 수정 (이미지 및 첨부파일 포함)
    @Transactional
    public void updateNotice(Long noticeId, NoticeDto noticeDto, MultipartFile image, MultipartFile attachment) throws IOException {
        log.info("공지사항 수정 시작 - ID: {}, 이미지: {}, 첨부파일: {}, 제거 플래그: {}", 
            noticeId,
            image != null ? image.getOriginalFilename() : "없음", 
            attachment != null ? attachment.getOriginalFilename() : "없음",
            noticeDto.isRemoveAttachment());
            
        // 기존 공지사항 조회
        NoticeDto existingNotice = noticeMapper.getNoticeById(noticeId);
        
        if (existingNotice == null) {
            throw new IllegalArgumentException("해당 공지사항을 찾을 수 없습니다: " + noticeId);
        }
        
        // 첨부파일 제거 요청이 있는 경우
        boolean removeAttachment = noticeDto.isRemoveAttachment();
        
        // 이미지 처리
        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제
            if (existingNotice.getImagePath() != null) {
                fileService.deleteImage(existingNotice.getImagePath());
            }
            // 새 이미지 저장
            String imagePath = fileService.saveImage(image);
            noticeDto.setImagePath(imagePath);
            log.info("이미지 업데이트 완료: {}", imagePath);
        } else if (removeAttachment && existingNotice.getImagePath() != null) {
            // 이미지 제거 요청이 있으면 삭제
            fileService.deleteImage(existingNotice.getImagePath());
            noticeDto.setImagePath(null);
            log.info("이미지 삭제 완료");
        } else {
            // 이미지 변경 없고 제거 요청도 없으면 기존 이미지 유지
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
            log.info("첨부파일 업데이트 완료: {}, 파일명: {}", attachmentPath, attachment.getOriginalFilename());
        } else if (removeAttachment && existingNotice.getAttachmentPath() != null) {
            // 첨부파일 제거 요청이 있으면 삭제
            fileService.deleteAttachment(existingNotice.getAttachmentPath());
            noticeDto.setAttachmentPath(null);
            noticeDto.setAttachmentName(null);
            log.info("첨부파일 삭제 완료");
        } else {
            // 첨부파일 변경 없고 제거 요청도 없으면 기존 정보 유지
            noticeDto.setAttachmentPath(existingNotice.getAttachmentPath());
            noticeDto.setAttachmentName(existingNotice.getAttachmentName());
        }
        
        // 수정자 정보 설정
        noticeDto.setLastModifiedBy(noticeDto.getMemberId());
        
        // 업데이트 시간은 Mapper에서 NOW()로 설정
        
        Notice notice = noticeDto.toEntity();
        noticeMapper.updateNotice(noticeId, notice);
        log.info("공지사항 수정 완료, ID: {}", noticeId);
    }

    // ✅ 공지사항 삭제 (관리자만 가능)
    @Transactional
    public void deleteNotice(Long noticeId) throws IOException {
        log.info("공지사항 삭제 시작 - ID: {}", noticeId);
        
        // 이미지 및 첨부파일 삭제를 위해 공지사항 조회
        NoticeDto notice = noticeMapper.getNoticeById(noticeId);
        
        if (notice != null) {
            // 이미지 삭제
            if (notice.getImagePath() != null) {
                fileService.deleteImage(notice.getImagePath());
                log.info("이미지 삭제 완료: {}", notice.getImagePath());
            }
            
            // 첨부파일 삭제
            if (notice.getAttachmentPath() != null) {
                fileService.deleteAttachment(notice.getAttachmentPath());
                log.info("첨부파일 삭제 완료: {}", notice.getAttachmentPath());
            }
        }
        
        noticeMapper.deleteNotice(noticeId);
        log.info("공지사항 삭제 완료, ID: {}", noticeId);
    }

    // ✅ 이메일을 통해 member_id 가져오기
    public Long getMemberIdByEmail(String email) {
        return memberMapper.getMemberByEmail(email).getMemberId();
    }
}