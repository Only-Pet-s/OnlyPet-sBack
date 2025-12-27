package com.op.back.lecture.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.op.back.auth.model.AuthUser;
import com.op.back.auth.model.User;
import com.op.back.lecture.dto.LectureCreateRequest;
import com.op.back.lecture.dto.LectureDetailResponse;
import com.op.back.lecture.dto.LectureListItemResponse;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.lecture.repository.UserRepository;
import com.op.back.lecture.search.LectureSearchDocument;
import com.op.back.lecture.search.LectureSearchRepository;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Service
public class LectureServiceImpl implements LectureService {
    private final LectureRepository lectureRepository;
    private final LectureSearchRepository lectureSearchRepository;
    private final UserRepository userRepository;
    private final LectureSearchService lectureSearchService;



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


        lecture.setCreatedAt(Instant.now());

        lectureRepository.save(lecture);

        //Elasticsearch 저장
        LectureSearchDocument doc = new LectureSearchDocument();
        doc.setLectureId(lecture.getLectureId());
        doc.setTitle(lecture.getTitle());
        doc.setDescription(lecture.getDescription());
        doc.setTags(lecture.getTags());
        doc.setCategory(lecture.getCategory());
        doc.setLecturerUid(currentUid);
        doc.setLecturerName(user.getNickname());
        //// TODO: 관리자 승인 후 true로 변경
        doc.setAdminApproved(true);
        doc.setPublished(true);
        doc.setRating(0.0);
        doc.setPrice(lecture.getPrice());

        lectureSearchRepository.save(doc);

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
                lecture.getCreatedAt()
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

    //S3 영상 강의 업로드
    @Override
    public void uploadVideo(String lectureId,MultipartFile video,String title,
            int order,boolean preview,String currentUid) {
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

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(video.getContentType())
                        .build(),
                RequestBody.fromBytes(video.getBytes())
        );

        String videoUrl = "https://" + bucketName + ".s3.amazonaws.com/" + key;

        // Firestore에 영상 메타데이터 저장
        LectureVideo lectureVideo = new LectureVideo();
        lectureVideo.setVideoId(videoId);
        lectureVideo.setLectureId(lectureId);
        lectureVideo.setTitle(title);
        lectureVideo.setOrder(order);
        lectureVideo.setVideoUrl(videoUrl);
        lectureVideo.setPreview(preview);
        lectureVideo.setCreatedAt(Instant.now());

        lectureRepository.saveVideo(lectureId, lectureVideo);

        // videoCount +1
        lectureRepository.incrementVideoCount(lectureId);
    }
}
