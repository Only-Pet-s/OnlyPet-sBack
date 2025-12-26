package com.op.back.myPage.controller;

import com.op.back.auth.dto.ResponseDTO;
import com.op.back.auth.util.JwtUtil;
import com.op.back.myPage.dto.*;
import com.op.back.myPage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/myPage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final JwtUtil jwtUtil;

    // 프로필 조회 -> pageVisible이 PUBLIC인 경우만 조회를 할 수 있다.
    @GetMapping("/{uid}")
    public ResponseEntity<MyPageDTO> getProfile(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {

        String loginUid = jwtUtil.getUidOrNull(authHeader);

        return ResponseEntity.ok(
                myPageService.getProfile(uid, loginUid)
        );
    }

    // 게시물 썸네일 목록 (공개)
    @GetMapping("/{uid}/posts")
    public ResponseEntity<List<MyPagePostDTO>> getMyPosts(
            @PathVariable String uid
    ) throws Exception {

        return ResponseEntity.ok(
                myPageService.getMyPosts(uid)
        );
    }

    // 숏츠 목록 조회
    @GetMapping("{uid}/shorts")
    public ResponseEntity<List<MyPageShortDTO>> getMyShorts(
            @PathVariable String uid
    )throws Exception {
        return ResponseEntity.ok(
                myPageService.getMyShorts(uid)
        );
    }

    // 공개 범위 변경, 로그인 uid == uid
    @PatchMapping("/pageVisible")
    public ResponseEntity<PageVisibleDTO> updatePageVisible(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PageVisibleRequestDTO request
    ){
        String uid = jwtUtil.getUid(authHeader.substring(7));

        myPageService.updatePageVisible(uid, request.getPageVisible());

        return ResponseEntity.ok(
                new PageVisibleDTO(request.getPageVisible())
        );
    }

    // 계정(판매자/강의자/펫시터 구분)
    @PostMapping(value = "/updateRole", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<?>> updateRole(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart(required = false) String seller,
            @RequestPart(required = false) String instructor,
            @RequestPart(required = false) String petsitter,
            @RequestPart(required = false) MultipartFile businessFile,
            @RequestPart(required = false) MultipartFile certificateFile
    ) {
        try {
            String token = authHeader.substring(7);
            String uid = jwtUtil.getUid(token);

            Boolean isSeller = seller != null ? Boolean.parseBoolean(seller) : null;
            Boolean isInstructor = instructor != null ? Boolean.parseBoolean(instructor) : null;
            Boolean isPetsitter = petsitter != null ? Boolean.parseBoolean(petsitter) : null;

            myPageService.updateRole(uid, isSeller, isInstructor, isPetsitter, businessFile, certificateFile);

            return ResponseEntity.ok(new ResponseDTO<>(true, "계정 수정 성공", null));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ResponseDTO<>(false, "계정 수정 실패: " + e.getMessage(), null));
        }
    }

    @PatchMapping("/updateInfo")
    public ResponseEntity<ResponseDTO<?>> update(@RequestBody Map<String, Object> data, @RequestHeader("Authorization") String header){
        try{
            String token = header.substring(7);
            String uid = jwtUtil.getUid(token);
            myPageService.updateUserInfo(uid, data);

            return ResponseEntity.ok(new ResponseDTO<>(true, "회원 정보 수정 성공", null));
        }catch(Exception e){
            return ResponseEntity.badRequest().body(new  ResponseDTO<>(false, "회원 정보 수정 실패: " + e.getMessage(), null));
        }
    }

    @PatchMapping(value="/updatePImg", consumes = "multipart/form-data")
    public ResponseEntity<ResponseDTO<?>> updateProfileImage(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("Authorization") String header
    ){
        try{
            String token = header.substring(7);
            String uid = jwtUtil.getUid(token);

            String imageUrl = myPageService.updateProfileImage(uid, file);

            Map<String, Object> result = new HashMap<>();
            result.put("profileImageUrl", imageUrl);

            return ResponseEntity.ok(
                    new ResponseDTO<>(true, "프로필 사진 변경", result)
            );
        }catch(Exception e){
            return ResponseEntity.badRequest().body(
                    new ResponseDTO<>(false, "프로필 사진 변경 실패" + e.getMessage(), null)
            );
        }
    }

    // 캡션 수정 하기
    @PatchMapping("/updateCaption")
    public ResponseEntity<ResponseDTO<?>> updateCaption(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> data
    ){
        try{
            String uid = jwtUtil.getUid(authHeader.substring(7));

            myPageService.updateCaption(uid, data);

            return ResponseEntity.ok(new ResponseDTO<>(true, "캡션 수정 성공", null));
        }catch(Exception e){
            return ResponseEntity.badRequest()
                    .body(new ResponseDTO<>(false, "캡션 수정 실패: " + e.getMessage(), null));
        }
    }
}