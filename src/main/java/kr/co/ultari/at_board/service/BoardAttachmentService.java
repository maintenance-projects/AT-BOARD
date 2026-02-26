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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * 파일 I/O가 DB 커넥션을 점유하지 않도록 @Transactional 제거.
     * DB 저장은 attachmentRepository.save()의 자체 트랜잭션 사용.
     */
    public BoardAttachment uploadAttachment(MultipartFile file, LocalDateTime expiresAt) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        long maxSize = appSettingService.getMaxAttachmentSize();
        log.info("uploadAttachment: fileSize={}, maxSize={}, expiresAt={}", file.getSize(), maxSize, expiresAt);
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

        return saveFile(file, originalFilename, extension, expiresAt);
    }

    /**
     * 파일 업로드 - 관리자용 (용량·확장자 제한 없음)
     * 파일 I/O가 DB 커넥션을 점유하지 않도록 @Transactional 제거.
     */
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

        return saveFile(file, originalFilename, extension, null);
    }

    private BoardAttachment saveFile(MultipartFile file, String originalFilename, String extension,
                                     LocalDateTime expiresAt) throws IOException {
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
                .expiresAt(expiresAt)
                .build();

        try {
            return attachmentRepository.save(attachment);
        } catch (Exception e) {
            // DB 저장 실패 시 업로드된 파일 정리
            deleteFile(relativeFilePath);
            throw e;
        }
    }

    /**
     * 첨부파일들에 게시글 ID 할당 (벌크 UPDATE - N+1 방지)
     */
    @Transactional("primaryTransactionManager")
    public void assignToBoard(List<Long> attachmentIds, Long boardId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        attachmentRepository.assignBoardIdByIds(boardId, attachmentIds);
    }

    public List<BoardAttachment> getByBoardId(Long boardId) {
        return attachmentRepository.findByBoardId(boardId);
    }

    @Transactional(value = "primaryTransactionManager", readOnly = true)
    public Set<Long> getBoardIdsWithAttachments(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) return Collections.emptySet();
        return new HashSet<>(attachmentRepository.findBoardIdsWithAttachments(boardIds));
    }

    /**
     * 단일 첨부파일 삭제 (DB 먼저, 파일은 트랜잭션 커밋 후 삭제)
     */
    @Transactional("primaryTransactionManager")
    public void deleteAttachment(Long id) {
        attachmentRepository.findById(id).ifPresent(attachment -> {
            final String filePath = attachment.getFilePath();
            attachmentRepository.delete(attachment);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFile(filePath);
                }
            });
        });
    }

    /**
     * 게시글의 모든 첨부파일 삭제 (DB 먼저, 파일은 트랜잭션 커밋 후 삭제)
     */
    @Transactional("primaryTransactionManager")
    public void deleteByBoardId(Long boardId) {
        List<BoardAttachment> attachments = attachmentRepository.findByBoardId(boardId);
        final List<String> filePaths = attachments.stream()
                .map(BoardAttachment::getFilePath)
                .collect(Collectors.toList());
        attachmentRepository.deleteAllByBoardId(boardId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String path : filePaths) {
                    deleteFile(path);
                }
            }
        });
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
