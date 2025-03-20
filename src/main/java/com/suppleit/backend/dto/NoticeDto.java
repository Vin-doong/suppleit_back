package com.suppleit.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.suppleit.backend.model.Notice;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeDto {
    private Long noticeId;
    private String title;
    private String content;
    private Long memberId;  // ✅ 작성자 ID 추가

    private String imagePath;
    private String attachmentPath;
    private String attachmentName;

    //조회수, 작성일, 수정일  //0320
    private int views;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 이미지 URL 생성 메서드 0319
    public String getImageUrl() {
        if (imagePath != null && !imagePath.isEmpty()) {
            return "/api/notice/image/" + imagePath;
        }
        return null;
    }

    // 첨부파일 URL 생성 메서드 0319
    public String getAttachmentUrl() {
        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            return "/api/notice/attachment/" + noticeId + "/" + attachmentName;
        }
        return null;
    }

    // ✅ Notice 엔티티 → NoticeDto 변환 메서드
    public static NoticeDto fromEntity(Notice notice) {
        return NoticeDto.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .memberId(notice.getMemberId())
                .build();
    }

    // ✅ NoticeDto → Notice 엔티티 변환 메서드
    public Notice toEntity() {
        return Notice.builder()
                .noticeId(this.noticeId)
                .title(this.title)
                .content(this.content)
                .memberId(this.memberId)
                .build();
    }
}
