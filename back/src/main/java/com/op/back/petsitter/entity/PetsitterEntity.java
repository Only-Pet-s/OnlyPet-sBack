package com.op.back.petsitter.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PetsitterEntity {

    private String petsitterId;
    private String name;
    private String profileImageUrl;

    private String region;
    private double lat;
    private double lng;

    private double rating;
    private double mannerTemp;

    private String caption;

    private boolean verified;

    private int career;
    private int completeCount;
    private int responseRatio;
    private int price;

    private boolean reserveAvailable;

    private boolean dog;
    private boolean cat;
    private boolean etc;
}
