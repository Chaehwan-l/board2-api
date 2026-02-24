package lch.global.infra;

import java.io.IOException;
import java.util.UUID;

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

 private final S3Client s3Client;

 @Value("${spring.cloud.aws.s3.bucket}") // 환경변수 S3_BUCKET_NAME 사용
 private String bucket;

 // S3Client는 spring-cloud-aws-starter-s3가 자동 구성(Auto-Configuration)하여 주입해 줍니다.
 public S3StorageService(S3Client s3Client) {
     this.s3Client = s3Client;
 }

 // 파일 업로드 후 저장된 S3 Key 반환
 public String uploadFile(MultipartFile file) {
     if (file.isEmpty() || file.getOriginalFilename() == null) {
         throw new BusinessException("빈 파일은 업로드할 수 없습니다.");
     }

     // 파일명 중복 방지를 위한 UUID 생성
     String extension = getFileExtension(file.getOriginalFilename());
     String s3Key = "board/" + UUID.randomUUID() + extension;

     try {
         PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                 .bucket(bucket)
                 .key(s3Key)
                 .contentType(file.getContentType())
                 .build();

         // S3로 파일 스트리밍 업로드 (Server Proxy 방식)
         s3Client.putObject(putObjectRequest,
                 RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

         return s3Key;

     } catch (IOException e) {
         throw new BusinessException("S3 파일 업로드 중 오류가 발생했습니다.");
     }
 }

 // S3 파일 삭제 (게시글 삭제 시 함께 호출)
 public void deleteFile(String s3Key) {
     try {
         DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                 .bucket(bucket)
                 .key(s3Key)
                 .build();
         s3Client.deleteObject(deleteRequest);
     } catch (Exception e) {
         // S3 삭제 실패는 메인 로직(게시글 삭제)을 롤백시키지 않도록 로깅만 하는 것이 실무적 관례입니다.
         System.err.println("S3 파일 삭제 실패: " + s3Key);
     }
 }

 private String getFileExtension(String fileName) {
     int dotIndex = fileName.lastIndexOf(".");
     if (dotIndex > 0) {
         return fileName.substring(dotIndex);
     }
     return "";
 }
}