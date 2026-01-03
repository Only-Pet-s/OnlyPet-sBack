package com.op.back.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

/**
 * 서버에서 영상 1프레임을 썸네일(JPEG)로 추출합니다.
 * - ffmpeg가 PATH에 설치되어 있어야 합니다.
 */

//ffmpeg로 영상에서 1프레임 추출하여 byte[]반환함
public class VideoThumbnailUtil {

    private VideoThumbnailUtil() {}

    public static byte[] extractJpegBytes(MultipartFile video) throws Exception {
        // 임시 영상 파일
        File tempVideo = File.createTempFile("video-", ".mp4");
        video.transferTo(tempVideo);

        // 임시 썸네일 파일 (확장자 중요)
        File tempImage = File.createTempFile("thumb-", ".jpg");

        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg",
            "-y",
            "-i", tempVideo.getAbsolutePath(),
            "-ss", "00:00:01",
            "-vframes", "1",
            tempImage.getAbsolutePath()   //반드시 .jpg
        );

        //stdout + stderr 합치기 (블로킹 방지)
        pb.redirectErrorStream(true);
        Process process = pb.start();

        //ffmpeg 출력 소비 (안 하면 멈춤)
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            while (br.readLine() != null) {
                // 로그 소비만 하면 됨
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("ffmpeg failed, exitCode=" + exitCode);
        }

        byte[] bytes = Files.readAllBytes(tempImage.toPath());

        //임시 파일 정리
        tempVideo.delete();
        tempImage.delete();

        return bytes;
    }
}
