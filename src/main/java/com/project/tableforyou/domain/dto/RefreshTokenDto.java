package com.project.tableforyou.domain.dto;

import com.project.tableforyou.domain.entity.RefreshToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class RefreshTokenDto {

    private String username;
    private String refreshToken;

    public RefreshToken toEntity() {
        return RefreshToken.builder()
                .username(username)
                .refreshToken(refreshToken)
                .build();
    }
}
