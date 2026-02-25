package kr.co.ultari.at_board.service;

import kr.co.ultari.at_board.model.primary.BoardAttachment;
import kr.co.ultari.at_board.repository.primary.BoardAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardAttachmentService {

    private final BoardAttachmentRepository attachmentRepository;
    private final AppSettingService appSettingService;

    @Value("${file.upload.path:uploads}")
    private String uploadPath;

    /**
     * 파일 업로드 후 DB에 임시 레코드(boardId=null) 생성 - 일반 사용자용 (용량·확장자 제한 적용)
     * getMaxAttachmentSize/getBlockedExtensions 는 각자 @Transactional(readOnly=true) 를 가지므로
     * uploadAttachment 의 primaryTransactionManager 트랜잭션에 참여하여 안전하게 읽음
     */
    @Transactional("primaryTransactionManager")
    public BoardAttachment uploadAttachment(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        long maxSize = appSettingService.getMaxAttachmentSize();
        log.info("uploadAttachment: fileSize={}, maxSize={}", file.getSize(), maxSize);
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기는 " + (maxSize / 1024 / 1024) + "MB를 초과할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = originalFilename.substring(lastDot + 1).toLowerCase();
        }

        java.util.Set<String> blockedExts = appSettingService.getBlockedExtensions();
        log.info("uploadAttachment: extension='{}', blockedExtensions={}", extension, blockedExts);
        if (blockedExts.contains(extension)) {
            throw new IllegalArgumentException("업로드할 수 없는 파일 형식입니다: ." + extension);
        }

        return saveFile(file, originalFilename, extension);
    }

    /**
     * 파일 업로드 - 관리자용 (용량·확장자 제한 없음)
     */
    @Transactional("primaryTransactionManager")
    public BoardAttachment uploadAttachmentAdmin(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0) {
            extension = originalFilename.substring(lastDot + 1).toLowerCase();
        }

        return saveFile(file, originalFilename, extension);
    }

    private BoardAttachment saveFile(MultipartFile file, String originalFilename, String extension) throws IOException {
        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path attachDir = Paths.get(uploadPath, "attach", dateFolder);
        Files.createDirectories(attachDir);

        String storedName = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
        Path filePath = attachDir.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String relativeFilePath = "attach/" + dateFolder + "/" + storedName;

        BoardAttachment attachment = BoardAttachment.builder()
                .boardId(null)
                .originalName(originalFilename)
                .storedName(storedName)
                .filePath(relativeFilePath)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        return attachmentRepository.save(attachment);
    }

    /**
     * 첨부파일들에 게시글 ID 할당
     */
    @Transactional("primaryTransactionManager")
    public void assignToBoard(List<Long> attachmentIds, Long boardId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            attachmentRepository.findById(attachmentId).ifPresent(attachment -> {
                // boardId가 null인 (아직 할당 안 된) 첨부파일만 처리
                if (attachment.getBoardId() == null) {
                    attachment.setBoardId(boardId);
                    attachmentRepository.save(attachment);
                }
            });
        }
    }

    public List<BoardAttachment> getByBoardId(Long boardId) {
        return attachmentRepository.findByBoardId(boardId);
    }

    /**
     * 단일 첨부파일 삭제 (파일 + DB)
     */
    @Transactional("primaryTransactionManager")
    public void deleteAttachment(Long id) {
        attachmentRepository.findById(id).ifPresent(attachment -> {
            deleteFile(attachment.getFilePath());
            attachmentRepository.delete(attachment);
        });
    }

    /**
     * 게시글의 모든 첨부파일 삭제
     */
    @Transactional("primaryTransactionManager")
    public void deleteByBoardId(Long boardId) {
        List<BoardAttachment> attachments = attachmentRepository.findByBoardId(boardId);
        for (BoardAttachment attachment : attachments) {
            deleteFile(attachment.getFilePath());
        }
        attachmentRepository.deleteAllByBoardId(boardId);
    }

    /**
     * 다운로드용 Resource 반환
     */
    public Resource getAttachmentFile(Long id) {
        BoardAttachment attachment = attachmentRepository.findById(id).orElse(null);
        if (attachment == null) {
            return null;
        }

        try {
            Path filePath = Paths.get(uploadPath).resolve(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", attachment.getFilePath(), e);
        }
        return null;
    }

    public BoardAttachment getById(Long id) {
        return attachmentRepository.findById(id).orElse(null);
    }

    private void deleteFile(String relativeFilePath) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(relativeFilePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativeFilePath, e);
        }
    }
}
