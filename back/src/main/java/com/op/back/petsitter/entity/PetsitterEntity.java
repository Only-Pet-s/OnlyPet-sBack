package com.op.back.petsitter.entity;

import com.op.back.petsitter.dto.PetsitterOperateTimeDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class PetsitterEntity {

    private String petsitterId;
    private String name;
    private String profileImageUrl;
    private String phone;

    private String address;
    private double lat;
    private double lng;
    private double distance;

    private double rating;
    private double mannerTemp;

    private String caption;

    private int career;
    private int completeCount;
    private int responseRatio;
    private int price;

    private boolean reserveAvailable;

    private boolean dog;
    private boolean cat;
    private boolean etc;
}
