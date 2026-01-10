package com.op.back.lecture.repository;

import java.util.Optional;

import com.op.back.auth.model.User;

public interface UserRepository {

    Optional<User> findByUid(String uid);
    void updateInstructorRole(String uid, boolean instructor);
}
