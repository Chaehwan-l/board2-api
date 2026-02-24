package lch.global.infra;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lch.global.error.BusinessException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

    // 로거 선언
    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BusinessException("빈 파일은 업로드할 수 없습니다.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String s3Key = "board/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3 파일 업로드 성공: {}", s3Key); // 정보 로그
            return s3Key;

        } catch (IOException e) {
            log.error("S3 파일 업로드 중 I/O 오류 발생: {}", e.getMessage());
            throw new BusinessException("S3 파일 업로드 중 오류가 발생했습니다.");
        }
    }

    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 파일 삭제 성공: {}", s3Key);
        } catch (Exception e) {
            // 삭제 실패 시에는 로직을 멈추지 않고 에러 로그만 남김
            log.error("S3 파일 삭제 실패 [key: {}]: {}", s3Key, e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0) ? fileName.substring(dotIndex) : "";
    }
}