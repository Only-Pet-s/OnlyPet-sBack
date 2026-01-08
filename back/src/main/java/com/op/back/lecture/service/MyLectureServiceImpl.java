package com.op.back.lecture.service;

import com.google.cloud.Timestamp;
import com.op.back.lecture.dto.LectureProgressUpdateRequest;
import com.op.back.lecture.dto.MyLectureItemResponse;
import com.op.back.lecture.dto.MyLectureSummaryResponse;
import com.op.back.lecture.model.Lecture;
import com.op.back.lecture.model.UserLecture;
import com.op.back.lecture.repository.LectureRepository;
import com.op.back.lecture.repository.UserLectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyLectureServiceImpl implements MyLectureService {

    private final UserLectureRepository userLectureRepository;
    private final LectureRepository lectureRepository;

    @Override
    public void enroll(String lectureId, String uid) {
        // 강의 존재 확인
        lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("강의 없음"));

        UserLecture userLecture = userLectureRepository.findById(uid, lectureId)
                .orElseGet(UserLecture::new);

        userLecture.setLectureId(lectureId);
        if (userLecture.getEnrolledAt() == null) {
            userLecture.setEnrolledAt(Timestamp.now());
        }
        if (userLecture.getLastWatchedAt() == null) {
            userLecture.setLastWatchedAt(Timestamp.now());
        }
        userLectureRepository.save(uid, userLecture);
    }

    @Override
    public MyLectureSummaryResponse getMyLectures(String uid) {
        List<UserLecture> items = userLectureRepository.findAll(uid);

        int studyingCount = 0;
        int completedCount = 0;
        int progressSum = 0;
        int totalSeconds = 0;

        List<MyLectureItemResponse> responses = items.stream().map(ul -> {
            Lecture lecture = lectureRepository.findById(ul.getLectureId()).orElse(null);
            String title = lecture != null ? lecture.getTitle() : "(삭제된 강의)";
            String thumb = lecture != null ? lecture.getThumbnailUrl() : null;
            String lecturerName = lecture != null ? lecture.getLecturerName() : null;

            Instant last = ul.getLastWatchedAt() != null ? ul.getLastWatchedAt().toDate().toInstant() : null;

            return new MyLectureItemResponse(
                    ul.getLectureId(),
                    title,
                    thumb,
                    lecturerName,
                    ul.getProgressPercent(),
                    ul.isCompleted(),
                    ul.getTotalWatchedSeconds(),
                    last.toString()
            );
        }).toList();

        for (UserLecture ul : items) {
            if (ul.isCompleted()) {
                completedCount++;
            } else {
                studyingCount++;
            }
            progressSum += Math.max(0, Math.min(100, ul.getProgressPercent()));
            totalSeconds += Math.max(0, ul.getTotalWatchedSeconds());
        }

        int averageProgress = items.isEmpty() ? 0 : (int) Math.round(progressSum / (double) items.size());
        int totalStudyMinutes = (int) Math.ceil(totalSeconds / 60.0);

        return new MyLectureSummaryResponse(
                studyingCount,
                completedCount,
                averageProgress,
                totalStudyMinutes,
                responses
        );
    }

    @Override
    public void updateProgress(String lectureId, String uid, LectureProgressUpdateRequest request) {
        // 수강 기록이 없으면 자동 생성(UX)
        UserLecture userLecture = userLectureRepository.findById(uid, lectureId)
                .orElseGet(() -> {
                    UserLecture ul = new UserLecture();
                    ul.setLectureId(lectureId);
                    ul.setEnrolledAt(Timestamp.now());
                    return ul;
                });

        if (request.watchedSecondsDelta() != null) {
            int next = Math.max(0, userLecture.getTotalWatchedSeconds() + request.watchedSecondsDelta());
            userLecture.setTotalWatchedSeconds(next);
        }

        if (request.progressPercent() != null) {
            int p = Math.max(0, Math.min(100, request.progressPercent()));
            userLecture.setProgressPercent(p);
            if (p >= 100) {
                userLecture.setCompleted(true);
            }
        }

        userLecture.setLastWatchedAt(Timestamp.now());
        userLectureRepository.save(uid, userLecture);
    }
}
