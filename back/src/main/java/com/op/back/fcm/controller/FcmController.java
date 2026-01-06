package com.op.back.fcm.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.fcm.dto.FcmTokenRequestDTO;
import com.op.back.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final JwtUtil jwtUtil;

    @PostMapping("/fcmToken")
    public void registerFcmToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody FcmTokenRequestDTO req
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));
        fcmService.registerToken(uid, req);
    }
}
