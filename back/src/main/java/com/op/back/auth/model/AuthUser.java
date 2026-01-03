package com.op.back.auth.model;

import lombok.Getter;

@Getter
public class AuthUser {

    private final String uid;
    private final String email;
    private final String nickname;

    private final boolean instructor;
    private final boolean seller;
    private final boolean petsitter;
    private final boolean admin;

    public AuthUser(User user) {
        this.uid = user.getUid();
        this.email = user.getEmail();
        this.nickname = user.getNickname();

        this.instructor = user.isInstructor();
        this.seller = user.isSeller();
        this.petsitter = user.isPetsitter();
        this.admin = user.isAdmin();
    }

    // ===== 권한 판단 메서드 =====
    public boolean canCreateLecture() {
        return instructor;
    }

    public boolean canSellProduct() {
        return seller;
    }

    public boolean canProvidePetsitting() {
        return petsitter;
    }

    public boolean isAdmin() {
        return admin;
    }
}
