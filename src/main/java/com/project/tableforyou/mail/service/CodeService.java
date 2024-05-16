package com.project.tableforyou.mail.service;

import com.project.tableforyou.domain.user.repository.UserRepository;
import com.project.tableforyou.handler.exceptionHandler.error.ErrorCode;
import com.project.tableforyou.handler.exceptionHandler.exception.CustomException;
import com.project.tableforyou.mail.MailType;
import com.project.tableforyou.mail.dto.CodeDto;
import com.project.tableforyou.utils.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.project.tableforyou.utils.redis.RedisProperties.CODE_EXPIRATION_TIME;
import static com.project.tableforyou.utils.redis.RedisProperties.CODE_KEY_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final RedisUtil redisUtil;
    private static final int RESEND_THRESHOLD_SECONDS = 2 * 60; // 2분을 초로 환산

    /* 회원가입 인증번호 확인 메서드. */
    public void sendCodeToMail(String email) {

        if(!checkEmail(email)) {
            throw new CustomException(ErrorCode.INVALID_MAIL_ADDRESS);
        }

        if(!checkRetryEmail(email)) {
            throw new CustomException(ErrorCode.ALREADY_MAIL_REQUEST);
        }

        String authCode = createCode();

        mailService.sendMail(email, authCode, MailType.CODE);

        String key = CODE_KEY_PREFIX + email;
        redisUtil.set(key, authCode);
        redisUtil.expire(key, CODE_EXPIRATION_TIME);
    }

    /* 이메일 검증 */
    private boolean checkEmail(String email) {
        return email != null && !email.equals("");
    }

    /* 인증번호 재전송 시간 확인 */
    private boolean checkRetryEmail(String email) {

        String key = CODE_KEY_PREFIX + email;
        if(!redisUtil.setExisted(key)) {
            return true;
        } else {
            long expireTime = redisUtil.getExpire(key, TimeUnit.SECONDS);
            return expireTime <= RESEND_THRESHOLD_SECONDS;
        }
    }

    /* 인증번호 만드는 메서드. */
    private String createCode() {
        try {
            Random random = SecureRandom.getInstanceStrong();   // 암호학적으로 안전한 무작위 수를 생성. 인증번호는 보안적으로 중요하기 SecureRandom 사용.
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 6; i++) {
                sb.append(random.nextInt(10));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.info("Failed to create secure random instance", e);
            throw new RuntimeException("Failed to generate secure random number", e);
        }
    }

    /* 인증번호 확인 메서드. */
    public boolean verifiedCode(String email, String code) {

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.ALREADY_EXISTS_MAIL);
        }

        String key = CODE_KEY_PREFIX + email;
        String storedCode = (String) redisUtil.get(key);

        if(storedCode != null && storedCode.equals(code)) {     // 유효시간 지나지 않음 + 입력 코드 일치
            redisUtil.del(key);
            log.info("Authentication code verified successfully: {}", email);
            return true;
        } else if(storedCode == null) {     // 유효시간 지나서 redis에 없음
            log.warn("Authentication code has expired: {}", email);
            throw new CustomException(ErrorCode.CODE_EXPIRED);
        } else {                            // 코드 일치하지 않음
            log.warn("Authentication code mismatch");
            throw new CustomException(ErrorCode.INVALID_CODE);
        }
    }
}