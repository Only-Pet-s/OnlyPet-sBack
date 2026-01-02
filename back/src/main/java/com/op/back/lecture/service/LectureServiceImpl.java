package com.op.back.lecture.service;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import com.op.back.auth.model.AuthUser;
import com.op.back.auth.model.User;
import com.op.back.lecture.dto.LectureCreateRequest;
import com.op.back.lecture.dto.LectureDetailResponse;
import com.op.back.lecture.dto.LectureListItemResponse;
import com.op.back.lecture.dto.LectureVideoResponse;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.LectureVideo;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.lecture.repository.UserRepository;
import com.op.back.lecture.search.LectureSearchDocument;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import org.springframework.web.multipart.MultipartFile;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class LectureServiceImpl implements LectureService {
    private final LectureRepository lectureRepository;
    private final UserRepository userRepository;
    private final LectureSearchService lectureSearchService;
    private final ElasticsearchClient elasticsearchClient;
    private final S3Client s3Client;
    private final String bucketName = "onlypets-lecture-video";



    //강의 테마 생성
    @Override
    public String createLecture(LectureCreateRequest req, String currentUid) {

        //사용자 조회 (Firestore)
        User user = userRepository.findByUid(currentUid)
                        .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        AuthUser authUser = new AuthUser(user);

        // 강의자 권한 체크
        if (!authUser.canCreateLecture()) {
            throw new IllegalStateException("강의자만 강의를 등록할 수 있습니다.");
        }

        //강의 생성
        Lecture lecture = new Lecture();
        lecture.setLectureId(UUID.randomUUID().toString());
        lecture.setTitle(req.title());
        lecture.setDescription(req.description());
        lecture.setCategory(req.category());
        lecture.setTags(req.tags());
        lecture.setPrice(req.price());

        lecture.setLecturerUid(currentUid);
        lecture.setLecturerName(user.getNickname()); //DB 기준

        lecture.setVideoCount(0);         //강의 테마 카운트
        lecture.setRating(0.0);
        lecture.setReviewCount(0);

        lecture.setAdminApproved(true);   // TODO: 관리자 승인 플로우 붙일 예정
        lecture.setPublished(true);


        lecture.setCreatedAt(Timestamp.now());

        lectureRepository.save(lecture);


        LectureSearchDocument doc = new LectureSearchDocument();
        doc.setLectureId(lecture.getLectureId());
        doc.setTitle(lecture.getTitle());
        doc.setDescription(lecture.getDescription());
        doc.setTags(lecture.getTags());
        doc.setCategory(lecture.getCategory());
        doc.setLecturerUid(currentUid);
        doc.setLecturerName(user.getNickname());
        doc.setAdminApproved(true);
        doc.setPublished(true);
        doc.setRating(0.0);
        doc.setPrice(lecture.getPrice());
        //Elasticsearch 저장
        try{
                elasticsearchClient.index(i -> i
                        .index("lecture-index")
                        .id(doc.getLectureId())
                        .document(doc)
                );
        } catch(Exception e){
                // Firestore는 성공했으므로 검색만 잠시 안 될 수 있음
                log.error("Elasticsearch indexing failed", e);
                }

        return lecture.getLectureId();
    }

     /**
     * 강의 테마 목록 조회 (기본)
     */
    @Override
    public List<LectureListItemResponse> getLectures(int limit, int offset) {
        return lectureRepository.findAllPublishedApproved(limit, offset)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    /**
     * 강의 상세 조회
     */
    @Override
    public LectureDetailResponse getLecture(String lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        return new LectureDetailResponse(
                lecture.getLectureId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getCategory(),
                lecture.getPrice(),
                lecture.getThumbnailUrl(),
                lecture.getLecturerUid(),
                lecture.getLecturerName(),
                lecture.isAdminApproved(),
                lecture.isPublished(),
                lecture.getTags(),
                lecture.getRating(),
                lecture.getReviewCount(),
                lecture.getCreatedAt().toDate().toInstant()
        );
    }


    /**
     * 특정 강의자의 강의 목록
     */
    @Override
    public List<LectureListItemResponse> getLecturesByLecturer(
            String lecturerUid, int limit, int offset) {

        return lectureRepository
                .findByLecturerUidPublishedApproved(lecturerUid, limit, offset)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    private LectureListItemResponse toListItem(Lecture lecture) {
        return new LectureListItemResponse(
                lecture.getLectureId(),
                lecture.getTitle(),
                lecture.getThumbnailUrl(),
                lecture.getLecturerUid(),
                lecture.getLecturerName(),
                lecture.getRating(),
                lecture.getPrice(),
                lecture.getTags()
        );
    }
    
    @Override
    public List<LectureListItemResponse> searchLectures(
            String keyword, List<String> tags, String category, int limit, int offset) {

        List<String> lectureIds =
                lectureSearchService.searchLectureIds(keyword, tags, category, limit, offset);

        return lectureIds.stream()
                .map(id -> lectureRepository.findById(id).orElse(null))
                .filter(l -> l != null)
                .map(this::toListItem)
                .toList();
    }

    /* 
    * 영상 강의 업로드 
    */
    @Override
    public void uploadVideo(String lectureId, MultipartFile video, MultipartFile thumbnail, String title, 
                String description, int order, boolean preview, String currentUid) {
        // 강의 테마 존재 확인
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의 테마 없음"));

        // 강의자 본인 확인
        if (!lecture.getLecturerUid().equals(currentUid)) {
            throw new IllegalStateException("본인 강의만 업로드 가능");
        }

        // S3 업로드
        String videoId = UUID.randomUUID().toString();
        String key = "lectures/" + currentUid + "/" + lectureId + "/" + videoId + ".mp4";

        try {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .contentType(video.getContentType())
                                .build(),
                        RequestBody.fromBytes(video.getBytes())
                );
        } catch (AwsServiceException | SdkClientException | IOException e) {
                e.printStackTrace();
        }

        String videoUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;

        //썸네일(thumnail) 처리
        String thumbnailUrl;
        try {
                String thumbKey = "lectures/" + currentUid + "/" + lectureId + "/" + videoId + "_thumb.jpg";

                byte[] thumbBytes;
                if (thumbnail != null && !thumbnail.isEmpty()) {
                        thumbBytes = thumbnail.getBytes();
                        String ct = thumbnail.getContentType() != null ? thumbnail.getContentType() : "image/jpeg";
                        s3Client.putObject(
                                software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(thumbKey)
                                        .contentType(ct)
                                        .build(),
                                software.amazon.awssdk.core.sync.RequestBody.fromBytes(thumbBytes)
                        );
        } else {
                // 영상에서 1프레임 추출 (ffmpeg 필요)
                thumbBytes = com.op.back.common.util.VideoThumbnailUtil.extractJpegBytes(video);
                s3Client.putObject(
                        software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(thumbKey)
                                .contentType("image/jpeg")
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(thumbBytes)
                );
        }

        thumbnailUrl = "https://" + bucketName + ".s3.amazonaws.com/" + thumbKey;
        } catch (Exception e) {
                throw new RuntimeException("강의 썸네일 업로드/생성 실패", e);
        }

        // Firestore에 영상 메타데이터 저장
        LectureVideo lectureVideo = new LectureVideo();
        lectureVideo.setVideoId(videoId);
        lectureVideo.setLectureId(lectureId);
        lectureVideo.setTitle(title);
        lectureVideo.setDescription(description);
        lectureVideo.setOrder(order);
        lectureVideo.setVideoUrl(videoUrl);
        lectureVideo.setPreview(preview);
        lectureVideo.setCreatedAt(Timestamp.now());

        lectureRepository.saveVideo(lectureId, lectureVideo);

        // videoCount +1
        lectureRepository.incrementVideoCount(lectureId);
    }

    /*
     * 강의 동영상 조회
    */
    @Override
    public List<LectureVideoResponse> getLectureVideos(String lectureId, String currentUid) {

        // 1. 강의 존재 확인
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의 없음"));

        // 2. 영상 목록 조회
        List<LectureVideo> videos =
                lectureRepository.findVideosByLectureId(lectureId);

        // 3. 구매 여부 판단 (지금은 간단히)
        boolean purchased = lecture.getLecturerUid().equals(currentUid)
                || lecture.getPrice() == 0;

        return videos.stream()
                .filter(v -> !v.isDeleted()) // 삭제된 영상 숨김
                .map(v -> new LectureVideoResponse(
                        v.getVideoId(),
                        v.getTitle(),
                        v.getDescription(),
                        v.getOrder(),
                        v.getVideoUrl(),
                        v.getThumbnailUrl(),
                        v.isPreview(),
                        purchased || v.isPreview(), // 미리보기는 구매 없이 가능
                        v.isDeleted(),
                        v.getCreatedAt().toDate().toInstant()
                ))
                .toList();
    }

    @Override
    public void updateVideo(String lectureId,String videoId,String title,String description,Integer order,
                Boolean preview, org.springframework.web.multipart.MultipartFile video, 
                org.springframework.web.multipart.MultipartFile thumbnail, String currentUid) {

    Lecture lecture = lectureRepository.findById(lectureId)
            .orElseThrow(() -> new IllegalArgumentException("강의 테마 없음"));

    if (!lecture.getLecturerUid().equals(currentUid)) {
        throw new IllegalStateException("본인 강의만 수정 가능");
    }

    LectureVideo lectureVideo = lectureRepository.findVideoById(lectureId, videoId)
            .orElseThrow(() -> new IllegalArgumentException("영상 없음"));

    java.util.Map<String, Object> updates = new java.util.HashMap<>();

    // 메타데이터 수정
    if (title != null) 
        updates.put("title", title);
    if (description != null) 
        updates.put("description", description);
    if (order != null) 
        updates.put("order", order);
    if (preview != null) 
        updates.put("preview", preview);

    // video 교체 (같은 key로 overwrite)
    if (video != null && !video.isEmpty()) {
        String key = "lectures/" + currentUid + "/" + lectureId + "/" + videoId + ".mp4";
        try {
            s3Client.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(video.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(video.getBytes())
            );
        } catch (Exception e) {
            throw new RuntimeException("강의 영상 업로드 실패", e);
        }
        String videoUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;
        updates.put("videoUrl", videoUrl);

        // 썸네일이 따로 안 오면 새 영상 기준으로 재생성
        if (thumbnail == null || thumbnail.isEmpty()) {
            try {
                byte[] thumbBytes = com.op.back.common.util.VideoThumbnailUtil.extractJpegBytes(video);
                String thumbKey = "lectures/" + currentUid + "/" + lectureId + "/" + videoId + "_thumb.jpg";
                s3Client.putObject(
                        software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(thumbKey)
                                .contentType("image/jpeg")
                                .build(),
                        software.amazon.awssdk.core.sync.RequestBody.fromBytes(thumbBytes)
                );
                updates.put("thumbnailUrl", "https://" + bucketName + ".s3.amazonaws.com/" + thumbKey);
            } catch (Exception e) {
                throw new RuntimeException("강의 썸네일 자동 생성 실패", e);
            }
        }
    }

    // thumbnail 교체
    if (thumbnail != null && !thumbnail.isEmpty()) {
        try {
            String thumbKey = "lectures/" + currentUid + "/" + lectureId + "/" + videoId + "_thumb.jpg";
            s3Client.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(thumbKey)
                            .contentType(thumbnail.getContentType() != null ? thumbnail.getContentType() : "image/jpeg")
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(thumbnail.getBytes())
            );
            updates.put("thumbnailUrl", "https://" + bucketName + ".s3.amazonaws.com/" + thumbKey);
        } catch (Exception e) {
            throw new RuntimeException("강의 썸네일 업로드 실패", e);
        }
    }

    if (!updates.isEmpty()) {
        lectureRepository.updateVideo(lectureId, videoId, updates);
    }
}
}
