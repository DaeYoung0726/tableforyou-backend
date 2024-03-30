package com.project.tableforyou.service;

import com.project.tableforyou.handler.exceptionHandler.CustomException;
import com.project.tableforyou.handler.exceptionHandler.ErrorCode;
import com.project.tableforyou.domain.entity.RefreshToken;
import com.project.tableforyou.domain.dto.RefreshTokenDto;
import com.project.tableforyou.jwt.JwtUtil;
import com.project.tableforyou.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.project.tableforyou.jwt.JwtProperties.REFRESH_COOKIE_VALUE;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /* redis에 저장 */
    @Transactional
    public void save(RefreshTokenDto refreshTokenDto) {

        RefreshToken refreshToken = refreshTokenDto.toEntity();
        refreshTokenRepository.save(refreshToken);
    }

    /* refreshToken으로 redis에서 불러오기 */
    @Transactional(readOnly = true)
    public RefreshTokenDto findByRefreshToken(String refreshToken) {

        RefreshToken findRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken).orElseThrow(() ->
                new CustomException(ErrorCode.REFRESHTOKEN_NOT_FOUND));

        RefreshTokenDto refreshTokenDto = RefreshTokenDto.builder()
                .username(findRefreshToken.getUsername())
                .refreshToken(findRefreshToken.getRefreshToken())
                .build();
        return refreshTokenDto;
    }

    /* redis에서 삭제 */
    @Transactional
    public void delete(String refreshToken) {
        RefreshToken findRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken).orElseThrow(() ->
                new CustomException(ErrorCode.REFRESHTOKEN_NOT_FOUND));

        refreshTokenRepository.delete(findRefreshToken);
    }

    /* accessToken 재발급 */
    public String accessTokenReIssue(String refreshToken) {

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        return jwtUtil.generateAccessToken(role, username);      // 재발급
    }

    /* Refresh token rotation(RTR) 사용 */
    public String refreshTokenReIssue(HttpServletResponse response, RefreshTokenDto refreshTokenDto, String refreshToken) {

        this.delete(refreshTokenDto.getRefreshToken());

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);

        String refreshTokenReIssue = jwtUtil.generateRefreshToken(role, username);

        RefreshTokenDto refreshTokenReIssueDto = RefreshTokenDto.builder()
                .username(username)
                .refreshToken(refreshTokenReIssue)
                .build();

        this.save(refreshTokenReIssueDto);

        return refreshTokenReIssue;
    }

}
