package com.suppleit.backend.controller;

import com.suppleit.backend.dto.NoticeDto;
import com.suppleit.backend.service.FileService;
import com.suppleit.backend.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController extends JwtSupportController {

    private final NoticeService noticeService;
    private final FileService fileService;

    // 공지사항 전체 조회
    @GetMapping
    public ResponseEntity<List<NoticeDto>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
    }

    // 특정 공지사항 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDto> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNoticeById(noticeId));
    }

    // 공지사항 이미지 조회
    @GetMapping("/image/{year}/{month}/{day}/{fileName:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String day,
            @PathVariable String fileName) {
        try {
            String imagePath = year + "/" + month + "/" + day + "/" + fileName;
            Path path = fileService.getImagePath(imagePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // 또는 적절한 미디어 타입
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 첨부파일 다운로드
    @GetMapping("/attachment/{noticeId}/{fileName:.+}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long noticeId, 
            @PathVariable String fileName) {
        try {
            NoticeDto notice = noticeService.getNoticeById(noticeId);
            
            if (notice == null || notice.getAttachmentPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path path = fileService.getAttachmentPath(notice.getAttachmentPath());
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // 파일명 인코딩 (한글 깨짐 방지)
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                        .replaceAll("\\+", "%20");
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 공지사항 등록 (이미지 및 첨부파일 포함)
    @PostMapping
    public ResponseEntity<?> createNotice(
            @RequestPart("notice") NoticeDto notice, 
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            HttpServletRequest request) {
        try {
            // JWT에서 현재 로그인한 사용자의 이메일을 가져옴
            String email = extractEmailFromToken(request);

            // 이메일을 이용하여 member_id 조회
            Long memberId = noticeService.getMemberIdByEmail(email);
            
            // 공지사항 등록 (memberId 추가)
            notice.setMemberId(memberId);
            noticeService.createNotice(notice, image, attachment);
            
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

    // 공지사항 수정 (이미지 및 첨부파일 포함)
    @PutMapping("/{noticeId}")
    public ResponseEntity<?> updateNotice(
            @PathVariable Long noticeId, 
            @RequestPart("notice") NoticeDto notice,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        try {
            noticeService.updateNotice(noticeId, notice, image, attachment);
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

    // 공지사항 삭제
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