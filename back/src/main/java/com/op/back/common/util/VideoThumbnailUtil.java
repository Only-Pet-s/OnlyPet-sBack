package com.op.back.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 서버에서 영상 1프레임을 썸네일(JPEG)로 추출합니다.
 * - ffmpeg가 PATH에 설치되어 있어야 합니다.
 */

//ffmpeg로 영상에서 1프레임 추출하여 byte[]반환함
public class VideoThumbnailUtil {

    private VideoThumbnailUtil() {}

    public static byte[] extractJpegBytes(MultipartFile videoFile) throws IOException {
        File tempVideo = Files.createTempFile("video_", ".tmp").toFile();
        File tempThumb = Files.createTempFile("thumb_", ".jpg").toFile();

        try {
            videoFile.transferTo(tempVideo);

            // 1초 지점 1프레임 (영상이 짧으면 -ss 0도 가능하지만, 실무에서 1초가 더 안정적)
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", tempVideo.getAbsolutePath(),
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-q:v", "2",
                    tempThumb.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process p = pb.start();
            try {
                int exit = p.waitFor();
                if (exit != 0 || !tempThumb.exists() || tempThumb.length() == 0) {
                    throw new IllegalStateException("썸네일 자동 생성 실패: ffmpeg 실행 결과가 비정상입니다. (서버에 ffmpeg 설치/PATH 확인 필요)");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("썸네일 생성 중단", e);
            }

            return Files.readAllBytes(tempThumb.toPath());
        } finally {
            // best-effort cleanup
            if (tempVideo.exists()) tempVideo.delete();
            if (tempThumb.exists()) tempThumb.delete();
        }
    }
}
