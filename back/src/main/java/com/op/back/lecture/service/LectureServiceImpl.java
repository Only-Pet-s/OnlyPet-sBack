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
        lecture.setPrice(req.price());
        lecture.setTags(req.tags());

        lecture.setLecturerUid(currentUid);
        lecture.setLecturerName(user.getNickname()); //DB 기준

        lecture.setAdminApproved(true);
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
        doc.setRating(lecture.getRating());
        doc.setPrice(lecture.getPrice());

        lectureSearchRepository.save(doc);

        return lecture.getLectureId();
    }

     /**
     * 강의 목록 조회 (기본)
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
}
