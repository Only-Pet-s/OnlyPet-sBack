package com.op.back.auth.dto;

import lombok.Data;
import java.util.List;

@Data
public class RegisterDTO {

    private String name;
    private String nickname;
    private String email;
    private String password;
    private String address;
    private String phone;

    private boolean seller = false;
    private boolean instructor = false;
    private boolean petsitter = false;

    private String businessNumber;

    private List<PetDTO> animals;

    private String captionTitle;
    private String captionContent;

    private String pageVisible;

    private int followerCount = 0;
    private int followingCount = 0;
    private int postCount = 0;
}
