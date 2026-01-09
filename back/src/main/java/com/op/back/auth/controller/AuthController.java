package com.op.back.auth.controller;

import com.op.back.auth.dto.LoginDTO;
import com.op.back.auth.dto.RegisterDTO;
import com.op.back.auth.dto.ResponseDTO;
import com.op.back.auth.service.AuthService;

import com.op.back.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;

    // 회원 가입
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

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<?>> login(@RequestBody LoginDTO loginDTO) {
        try{
            Map<String, Object> data = authService.login(loginDTO);
            return ResponseEntity.ok(new ResponseDTO<>(true, "로그인 성공", data));
        }catch(Exception e){
            return ResponseEntity.status(401).body(new ResponseDTO<>(false, "로그인 실패: " + e.getMessage(), null));
        }
    }

    // 리프레시 토큰으로 액세스 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDTO<?>> refresh(@RequestBody Map<String, String> data) {
        try{
            String uid = data.get("uid");
            String refreshToken  = data.get("refreshToken");

            Map<String, Object> newTokens = authService.refresh(uid, refreshToken);

            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "토큰 재발급 성공", newTokens)
            );
        }catch(Exception e){
            return ResponseEntity.status(401).body(new ResponseDTO<>(false, "토큰 재발급 실패"+e.getMessage(), null));
        }
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<?>> logout(@RequestBody Map<String, String> data) {
        try{
            String uid = data.get("uid");
            authService.logout(uid);
            return ResponseEntity.ok(new ResponseDTO<>(true, "로그아웃 성공", null));
        }catch(Exception e){
            return ResponseEntity.badRequest().body(new ResponseDTO<>(false, "로그아웃 실패:"+e.getMessage(), null));
        }
    }

    //현재 비밀번호 타당성 검사
    @PostMapping("/pwValidation")
    public ResponseEntity<ResponseDTO<?>> pwValidation(
            @RequestBody Map<String, String> data,
            @RequestHeader("Authorization") String authHeader
    ) {
        try{
            String token = authHeader.substring(7);
            String email = jwtUtil.getEmail(token);
            String pw = data.get("password");

            if(!authService.pwValidation(email,pw)){
                return ResponseEntity.ok(
                        new ResponseDTO<>(true, "현재 비밀번호와 일치하지 않음", null)
                );
            }
            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "현재 비밀번호와 일치함", null)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, "현재 비밀번호와 일치하지 않음", null)
            );
        }
    }
    // 비밀번호 변경하기
    @PostMapping("/changePw")
    public ResponseEntity<ResponseDTO<?>> changePw(
            @RequestBody Map<String,String> data,
            @RequestHeader("Authorization") String authorizationHeader
    ){
        try{
            String token = authorizationHeader.substring(7);
            String uid = jwtUtil.getUid(token);
            String email = jwtUtil.getEmail(token);

            String oldPw = data.get("oldPassword");
            String newPw = data.get("newPassword");
            System.out.println(uid+email+oldPw+newPw);

            authService.changePassword(uid, email, oldPw, newPw);

            return ResponseEntity.ok(new ResponseDTO<>(true, "비밀번호 변경 성공", null));

        }catch(Exception e){
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, "비밀번호 변경 실패: "+e.getMessage(), null)
            );
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDTO<?>> deleteUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUid(token);

            authService.deleteUser(uid);

            return ResponseEntity.ok(new ResponseDTO<>(true, "회원 탈퇴 완료", null));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseDTO<>(false, "회원 탈퇴 실패: " + e.getMessage(), null));
        }
    }
}
