package com.suppleit.backend.controller;

import com.suppleit.backend.dto.NoticeDto;
import com.suppleit.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController extends JwtSupportController {

    private final NoticeService noticeService;

    // 공지사항 전체 조회 (모든 사용자 접근 가능)
    @GetMapping
    public ResponseEntity<List<NoticeDto>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    // 특정 공지사항 조회 (모든 사용자 접근 가능)
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDto> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNoticeById(noticeId));
    }

    // 공지사항 등록 (관리자만 가능)
    @PostMapping
    public ResponseEntity<?> createNotice(@RequestBody NoticeDto notice, HttpServletRequest request) {
        try {
            // JWT에서 현재 로그인한 사용자의 이메일을 가져옴
            String email = extractEmailFromToken(request);

            // 이메일을 이용하여 member_id 조회
            Long memberId = noticeService.getMemberIdByEmail(email);
            
            // 공지사항 등록 (memberId 추가)
            notice.setMemberId(memberId);
            noticeService.createNotice(notice);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "공지사항이 등록되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // 공지사항 수정 (관리자만 가능)
    @PutMapping("/{noticeId}")
    public ResponseEntity<?> updateNotice(@PathVariable Long noticeId, @RequestBody NoticeDto notice) {
        try {
            noticeService.updateNotice(noticeId, notice);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "공지사항이 수정되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // 공지사항 삭제 (관리자만 가능)
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<?> deleteNotice(@PathVariable Long noticeId) {
        try {
            noticeService.deleteNotice(noticeId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "공지사항이 삭제되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}