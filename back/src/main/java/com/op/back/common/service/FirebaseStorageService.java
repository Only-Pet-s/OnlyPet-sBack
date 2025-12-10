package com.op.back.common.service;

import com.google.firebase.cloud.StorageClient;
import com.google.cloud.storage.Bucket;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FirebaseStorageService {

    //Storage파일 업로드 후, public URL 반환
    public String uploadFile(MultipartFile file, String path) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Bucket bucket = StorageClient.getInstance().bucket();
        String fileName = path + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        bucket.create(fileName, file.getBytes(), file.getContentType());

        return String.format(
                "https://storage.googleapis.com/%s/%s",
                bucket.getName(),
                fileName
        );
    }

    //스토리지 삭제
    public void deleteFile(String mediaUrl) {
        try {
            if (mediaUrl == null || mediaUrl.isEmpty()) return;

            String bucketName = StorageClient.getInstance().bucket().getName();
            String prefix = "https://storage.googleapis.com/" + bucketName + "/";

            if (!mediaUrl.startsWith(prefix)) return;

            String filePath = mediaUrl.substring(prefix.length());

            Bucket bucket = StorageClient.getInstance().bucket();
            boolean deleted = bucket.get(filePath).delete();

            System.out.println("Storage file delete: " + deleted + " (" + filePath + ")");

        } catch (Exception e) {
            System.out.println("Storage delete error: " + e.getMessage());
        }
    }
}
