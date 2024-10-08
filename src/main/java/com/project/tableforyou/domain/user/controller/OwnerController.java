package com.project.tableforyou.domain.user.controller;

import com.project.tableforyou.common.handler.exceptionHandler.error.ErrorCode;
import com.project.tableforyou.common.handler.exceptionHandler.exception.CustomException;
import com.project.tableforyou.common.utils.api.ApiUtil;
import com.project.tableforyou.domain.reservation.dto.QueueReservationReqDto;
import com.project.tableforyou.domain.reservation.entity.TimeSlot;
import com.project.tableforyou.domain.reservation.service.OwnerReservationFacade;
import com.project.tableforyou.domain.reservation.service.OwnerReservationService;
import com.project.tableforyou.domain.restaurant.dto.RestaurantRequestDto;
import com.project.tableforyou.domain.restaurant.dto.RestaurantUpdateDto;
import com.project.tableforyou.domain.restaurant.service.OwnerRestaurantFacade;
import com.project.tableforyou.domain.user.apl.OwnerApi;
import com.project.tableforyou.security.auth.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/owner/restaurants")
@RequiredArgsConstructor
public class OwnerController implements OwnerApi {

    private final OwnerReservationService ownerReservationService;
    private final OwnerRestaurantFacade ownerRestaurantFacade;
    private final OwnerReservationFacade ownerReservationFacade;

    /* 가게 생성 */
    @Override
    @PostMapping
    public ResponseEntity<?> createRestaurant(@Valid @RequestPart(value = "dto") RestaurantRequestDto dto,
                                              @AuthenticationPrincipal PrincipalDetails principalDetails,
                                              @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
                                              @RequestPart(value = "subImages", required = false) List<MultipartFile> subImages) {

        return ResponseEntity.ok(ApiUtil.from(ownerRestaurantFacade.createRestaurant(
                principalDetails.getUsername(),
                dto,
                mainImage,
                subImages
        )));
    }

    /* 사장 가게 불러오기 */
    @Override
    @GetMapping
    public ResponseEntity<?> readRestaurant(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        return ResponseEntity.ok(ownerRestaurantFacade.getRestaurantsByOwner(
                principalDetails.getUsername()
        ));
    }

    /* 승인 거절된 가게 불러오기 */
    @Override
    @GetMapping("/rejected")
    public ResponseEntity<?> readRejectedRestaurant(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        return ResponseEntity.ok(ownerRestaurantFacade.getRejectedRestaurants(
                principalDetails.getUsername()
        ));
    }

    /* 가게 메인 이미지 업데이트 */
    @PatchMapping("/{restaurantId}/main-image")
    public ResponseEntity<?> updateMainImage(@PathVariable(name = "restaurantId") Long restaurantId,
                                             @RequestPart(value = "mainImage") MultipartFile mainImage) {

        ownerRestaurantFacade.updateMainImage(restaurantId, mainImage);
        return ResponseEntity.ok(ApiUtil.from("가게 메인 이미지 수정 완료."));
    }

    /* 가게 서브 이미지 업데이트 */
    @PatchMapping("/{restaurantId}/sub-image")
    public ResponseEntity<?> updateSubImage(@PathVariable(name = "restaurantId") Long restaurantId,
                                            @RequestPart(value = "deleteImageUrls", required = false) List<String> deleteImageUrls,
                                            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {

        ownerRestaurantFacade.updateSubImages(restaurantId, deleteImageUrls, newImages);
        return ResponseEntity.ok(ApiUtil.from("가게 서브 이미지 수정 완료."));
    }

    /* 가게 업데이트 */
    @Override
    @PutMapping("/{restaurantId}")
    public ResponseEntity<?> updateRestaurant(@Valid @RequestBody RestaurantUpdateDto restaurantUpdateDto,
                                                   @PathVariable(name = "restaurantId") Long restaurantId) {

        ownerRestaurantFacade.updateRestaurant(restaurantId, restaurantUpdateDto);
        return ResponseEntity.ok(ApiUtil.from("가게 수정 완료."));
    }

    /* 가게 삭제 */
    @Override
    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<?> deleteRestaurant(@PathVariable(name = "restaurantId") Long restaurantId) {

        ownerRestaurantFacade.deleteRestaurant(restaurantId);
        return ResponseEntity.ok(ApiUtil.from("가게 삭제 완료."));

    }

    /* 좌석 업데이트 */
    @Override
    @PatchMapping("/{restaurantId}/update-used-seats")
    public ResponseEntity<?> updateFullUsedSeats(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                 @PathVariable(name = "restaurantId") Long restaurantId,
                                                 @RequestParam("increase") boolean increase) {

        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername())) {
            return ResponseEntity.ok(ApiUtil.from(ownerReservationFacade.updateUsedSeats(restaurantId, increase)));
        } else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /* 해당 가게 예약자 불러오기. (번호표) */
    @Override
    @GetMapping("/{restaurantId}/queue-reservations")
    public ResponseEntity<?> readAllRestaurant(@PathVariable(name = "restaurantId") Long restaurantId,
                                                          @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername()))
            return ResponseEntity.ok(ownerReservationFacade.getQueueReservations(restaurantId));
        else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /* 예약 순서 미루기 (번호표) */
    @Override
    @PatchMapping("/{restaurantId}/queue-reservations/postponed-guest-booking/{username}")
    public ResponseEntity<?> postponedGuestBooking(@PathVariable(name = "restaurantId") Long restaurantId,
                                                        @PathVariable(name = "username") String username,
                                                        @RequestBody QueueReservationReqDto reservationDto,
                                                        @AuthenticationPrincipal PrincipalDetails principalDetails) {

        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername())) {
            ownerReservationFacade.postponeReservation(
                    restaurantId,
                    username,
                    reservationDto
            );
            return ResponseEntity.ok(ApiUtil.from("예약자 미루기 + 앞당기기 성공."));
        } else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /* 예약자 삭제 (번호표) */
    @Override
    @DeleteMapping("/{restaurantId}/queue-reservations/{username}")
    public ResponseEntity<?> deleteReservation(@PathVariable(name = "restaurantId") Long restaurantId,
                                                    @PathVariable(name = "username") String username,
                                                    @AuthenticationPrincipal PrincipalDetails principalDetails) {

        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername())) {
            ownerReservationFacade.deleteQueueReservation(
                    restaurantId,
                    username
            );
            return ResponseEntity.ok(ApiUtil.from("예약자 삭제 성공."));
        } else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /* 특정 시간 예약자 전체 불러오기 (특정 시간) */
    @Override
    @GetMapping("/{restaurantId}/timeslot-reservations")
    public ResponseEntity<?> readAllTimeSlotReservation(@PathVariable(name = "restaurantId") Long restaurantId,
                                                        @RequestParam(value = "date") String date,
                                                        @RequestParam(value = "time-slot") TimeSlot timeSlot,
                                                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername()))
            return ResponseEntity.ok(ownerReservationFacade.getTimeSlotReservations(
                    restaurantId,
                    date,
                    timeSlot
            ));
        else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    /* 예약 삭제하기 (특정 시간)*/
    @Override
    @DeleteMapping("/{restaurantId}/timeslot-reservations/{username}")
    public ResponseEntity<?> deleteReservation(@PathVariable(name = "restaurantId") Long restaurantId,
                                               @PathVariable(name = "username") String username,
                                               @RequestParam(value = "date") String date,
                                               @RequestParam(value = "time-slot") TimeSlot timeSlot,
                                               @AuthenticationPrincipal PrincipalDetails principalDetails) {

        if (ownerReservationService.isOwnerRestaurant(restaurantId, principalDetails.getUsername())) {
            ownerReservationFacade.deleteTimeSlotReservation(
                    restaurantId,
                    username,
                    date,
                    timeSlot
            );
            return ResponseEntity.ok(ApiUtil.from("예약자 삭제 성공."));
        } else
            throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
