package com.project.tableforyou.domain.visit.controller;

import com.project.tableforyou.domain.visit.api.VisitApi;
import com.project.tableforyou.domain.visit.service.VisitService;
import com.project.tableforyou.security.auth.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class VisitController implements VisitApi {

    private final VisitService visitService;

    /* 사용자가 방문한 가게 불러오기 */
    @Override
    @GetMapping("/restaurants")
    public ResponseEntity<?> readVisitRestaurant(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        return ResponseEntity.ok(visitService.userVisitRestaurants(principalDetails.getUsername()));
    }
}
