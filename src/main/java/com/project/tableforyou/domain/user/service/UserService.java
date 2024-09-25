package com.project.tableforyou.domain.user.service;

import com.project.tableforyou.domain.common.service.AssociatedEntityService;
import com.project.tableforyou.domain.user.dto.PasswordDto;
import com.project.tableforyou.domain.user.dto.SignUpDto;
import com.project.tableforyou.domain.user.dto.UserResponseDto;
import com.project.tableforyou.domain.user.dto.UserUpdateDto;
import com.project.tableforyou.domain.user.entity.Role;
import com.project.tableforyou.domain.user.entity.User;
import com.project.tableforyou.domain.user.repository.UserRepository;
import com.project.tableforyou.handler.exceptionHandler.error.ErrorCode;
import com.project.tableforyou.handler.exceptionHandler.exception.CustomException;
import com.project.tableforyou.security.auth.PrincipalDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AssociatedEntityService associatedEntityService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final PrincipalDetailsService principalDetailsService;

    private static final String USERNAME_PATTERN = "^[ㄱ-ㅎ가-힣a-z0-9-_]{4,20}$";
    private static final String NICKNAME_PATTERN = "^[ㄱ-ㅎ가-힣a-zA-Z0-9-_]{2,10}$";

    /* 회원 추가 */
    @Transactional
    public Long signUp(SignUpDto signUpDto) {

        signUpDto.setPassword(bCryptPasswordEncoder.encode(signUpDto.getPassword()));
        User user = signUpDto.toEntity();

        userRepository.save(user);

        return user.getId();
    }

    /* 회원 불러오기 */
    @Transactional(readOnly = true)
    public UserResponseDto readUser(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserResponseDto(user);
    }

    /* 회원 업데이트 */
    @Transactional
    public void updateUser(String username, UserUpdateDto userUpdateDto) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));
        userUpdateDto.setPassword(bCryptPasswordEncoder.encode(userUpdateDto.getPassword()));

        user.update(userUpdateDto.getNickname(), userUpdateDto.getPassword());

        principalDetailsService.updateUserCache(user);  // 정보 업데이트 시 사용자 캐시 업데이트
    }

    /* 회원 삭제 */
    @Transactional
    public void deleteUser(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        associatedEntityService.deleteAllLikeByUser(user);  // 회원 좋아요 삭제
        associatedEntityService.deleteAllVisitByUser(user); // 회원 방문가게 삭제

        if (user.getRole().equals(Role.OWNER)) {      // 사장이라면 회원 가게 삭제
            associatedEntityService.deleteAllRestaurantByUser(user);
        }

        userRepository.delete(user);
    }

    /* 비밀번호 검사 */
    @Transactional(readOnly = true)
    public boolean checkPass(String username, PasswordDto passwordDto) {
        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        return bCryptPasswordEncoder.matches(passwordDto.getPassword(), user.getPassword());
    }

    /* 아이디 중복 확인 */
    public boolean existsByUsername(String username) {

        if(!username.matches(USERNAME_PATTERN)) {
            throw new CustomException(ErrorCode.INVALID_USERNAME_PATTERN);
        }
        return userRepository.existsByUsername(username);
    }

    /* 닉네임 중복 확인 */
    public boolean existsByNickname(String nickname) {

        if (!nickname.matches(NICKNAME_PATTERN)) {
            throw new CustomException(ErrorCode.INVALID_NICKNAME_PATTERN);
        }
        return userRepository.existsByNickname(nickname);
    }
}