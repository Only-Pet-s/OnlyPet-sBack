package com.op.back.auth.model;

import lombok.Data;

@Data
public class User {
    private String uid;
    private String email;
    private String nickname;

    private boolean instructor;
    private boolean seller;
    private boolean petsitter;
    private boolean admin;
}