package kr.co.ultari.at_board.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    /**
     * 이미지 파일 저장 (Quill 에디터용)
     *
     * @param file         업로드 파일
     * @param relativePath 저장 상대 경로 (예: imgs/2024/01/01/uuid.jpg)
     * @return 웹 접근 경로 (예: /api/files/images/imgs/2024/01/01/uuid.jpg)
     */
    String uploadImage(MultipartFile file, String relativePath) throws IOException;

    /**
     * 첨부파일 저장
     *
     * @param file         업로드 파일
     * @param relativePath 저장 상대 경로 (예: attach/2024/01/01/uuid.pdf)
     * @return 저장된 상대 경로 (DB 저장용, 입력값과 동일)
     */
    String uploadAttachment(MultipartFile file, String relativePath) throws IOException;

    /**
     * 파일 로드 (이미지 서빙 또는 첨부파일 다운로드)
     *
     * @param relativePath 상대 경로 (예: imgs/2024/01/01/uuid.jpg 또는 attach/2024/01/01/uuid.pdf)
     */
    Resource loadFile(String relativePath) throws IOException;

    /**
     * 파일 삭제
     *
     * @param relativePath 상대 경로
     */
    void deleteFile(String relativePath);
}
