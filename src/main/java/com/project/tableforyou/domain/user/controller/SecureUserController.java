package com.project.tableforyou.domain.user.controller;

import com.project.tableforyou.domain.like.service.LikeService;
import com.project.tableforyou.domain.user.apl.SecureUserApi;
import com.project.tableforyou.domain.user.dto.PasswordDto;
import com.project.tableforyou.domain.user.dto.UserUpdateDto;
import com.project.tableforyou.domain.user.service.UserService;
import com.project.tableforyou.security.auth.PrincipalDetails;
import com.project.tableforyou.utils.api.ApiUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SecureUserController implements SecureUserApi {

    private final UserService userService;
    private final LikeService likeService;


    /* 회원 불러오기 */
    @Override
    @GetMapping
    public ResponseEntity<?> readUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return ResponseEntity.ok(userService.readUser(principalDetails.getUsername()));
    }

    /* 현재 비밀번호 검사 */
    @Override
    @PostMapping("/check-password")
    public ResponseEntity<?> checkPassword(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                 @RequestBody PasswordDto passwordDto) {

        return ResponseEntity.ok(ApiUtil.from(userService.checkPass(principalDetails.getUsername(), passwordDto)));
    }

    /* 회원 업데이트 */
    @Override
    @PutMapping
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateDto userUpdateDto,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {

        userService.updateUser(principalDetails.getUsername(), userUpdateDto);
        return ResponseEntity.ok(ApiUtil.from("회원 업데이트 성공."));

    }

    /* 회원 삭제 */
    @Override
    @DeleteMapping
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        userService.deleteUser(principalDetails.getId());
        return ResponseEntity.ok(ApiUtil.from("회원 삭제 성공."));
    }

    /* 좋아요한 가게 불러오기 */
    @Override
    @GetMapping("/like-restaurants")
    public ResponseEntity<?> getRestaurantLike(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        return ResponseEntity.ok(likeService.getRestaurantLike(principalDetails.getUsername()));
    }
}