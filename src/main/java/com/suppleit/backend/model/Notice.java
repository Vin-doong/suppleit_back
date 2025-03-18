package com.suppleit.backend.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {
    private Long noticeId;
    private String title;
    private String content;
    private Long memberId; // ✅ 추가: 작성자 ID (외래키)
}
