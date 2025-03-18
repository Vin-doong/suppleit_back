package com.suppleit.backend.dto;

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
