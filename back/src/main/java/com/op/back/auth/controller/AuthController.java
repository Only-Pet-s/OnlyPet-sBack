package com.op.back.auth.controller;

import com.op.back.auth.dto.RegisterDTO;
import com.op.back.auth.dto.ResponseDTO;
import com.op.back.auth.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDTO<?>> register(
            @RequestParam("data") String data,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "certificateFile", required = false) MultipartFile certificateFile
    ) {
        try {
            // JSON 문자열을 Java 객체로 변환
            ObjectMapper mapper = new ObjectMapper();
            RegisterDTO registerDTO = mapper.readValue(data, RegisterDTO.class);

            String uid = authService.registerUser(registerDTO, profileImage, certificateFile);
            return ResponseEntity.ok(new ResponseDTO<>(true, "회원가입 성공", uid));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, e.getMessage(), null));
        }
    }

}
