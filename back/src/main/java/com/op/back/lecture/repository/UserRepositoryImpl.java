package com.op.back.lecture.repository;

import com.google.cloud.firestore.Firestore;
import com.op.back.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final Firestore firestore;

    @Override
    public Optional<User> findByUid(String uid) {
        try {
            var snapshot = firestore
                    .collection("users")
                    .document(uid)
                    .get()
                    .get();

            return snapshot.exists()
                    ? Optional.of(snapshot.toObject(User.class))
                    : Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateInstructorRole(String uid, boolean instructor) {
        firestore.collection("users")
                .document(uid)
                .update("instructor", instructor);
    }
}
