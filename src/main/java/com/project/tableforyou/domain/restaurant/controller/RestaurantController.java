package com.project.tableforyou.domain.restaurant.controller;import com.project.tableforyou.domain.restaurant.dto.RestaurantRequestDto;import com.project.tableforyou.domain.restaurant.dto.RestaurantResponseDto;import com.project.tableforyou.domain.restaurant.dto.RestaurantUpdateDto;import com.project.tableforyou.domain.restaurant.service.RestaurantService;import com.project.tableforyou.security.auth.PrincipalDetails;import com.project.tableforyou.utils.redis.RedisUtil;import jakarta.servlet.ServletException;import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpServletResponse;import jakarta.validation.Valid;import lombok.Getter;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;import org.springframework.data.domain.Page;import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Sort;import org.springframework.data.web.PageableDefault;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.validation.BindingResult;import org.springframework.web.bind.annotation.*;import java.io.IOException;import java.util.Map;@RestController@RequestMapping("/restaurant")@RequiredArgsConstructor@Slf4jpublic class RestaurantController {    private final RestaurantService restaurantService;    private final RedisUtil redisUtil;    /* 가게 생성 */     // 추후에 관리자가 생성 권한을 가지니 save로 날리는 게 아닌 저장시키고 관리자 페이지에서 보고 생성해야함.    @PostMapping("/create")    public ResponseEntity<Object> create(@AuthenticationPrincipal PrincipalDetails principalDetails,    // 순서 조심                                         @Valid @RequestBody RestaurantRequestDto dto,                                         BindingResult bindingResult) {        try {            if (bindingResult.hasErrors()) {                Map<String, String> errors = restaurantService.validateHandler(bindingResult);                log.info("Failed to sign up: {}", errors);                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);            }            restaurantService.save(principalDetails.getUsername(), dto);            return ResponseEntity.ok("가게 생성 완료.");        } catch (Exception e) {            log.error("Error occurred during create Restaurant: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred during create Restaurant");        }    }    /* 가게 불러오기 */    @GetMapping("/{restaurant}")    public RestaurantResponseDto read(@PathVariable(name = "restaurant") String restaurant) {        return restaurantService.findByName(restaurant);    }    /* 전체 가게 불러오기. 페이징 처리 + 검색 기능 */    @GetMapping    public Page<RestaurantResponseDto> readAll(@PageableDefault(size = 20, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable,                                                @RequestParam(required = false) String searchKeyword) {        if(searchKeyword == null)            return restaurantService.RestaurantPageList(pageable);        else            return restaurantService.RestaurantPageSearchList(searchKeyword, searchKeyword, pageable);    }    /* 가게 업데이트 */    @PutMapping("/{restaurant}")    public ResponseEntity<String> update(@PathVariable(name = "restaurant") String restaurant, @RequestBody RestaurantUpdateDto dto,                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {        restaurantService.update(restaurant, principalDetails.getUsername(), dto);        return ResponseEntity.ok("가게 수정 완료.");    }    /* 가게 삭제 */    @DeleteMapping("/{restaurant}")    public ResponseEntity<String> delete(@PathVariable(name = "restaurant") String restaurant,                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {        restaurantService.delete(restaurant, principalDetails.getUsername());        return ResponseEntity.ok("가게 삭제 완료.");    }    /* 가게 평점 업데이트*/    @PatchMapping("/{restaurant}/update-rating")    public ResponseEntity<String> updateRating(@PathVariable(name = "restaurant") String restaurant, @RequestBody RatingDto rating) {        try {            restaurantService.updateRating(restaurant, rating.getRating());            return ResponseEntity.ok("가게 평점 업데이트 완료.");        } catch (Exception e) {            log.error("Error occurred while updating restaurant rating: {}", e.getMessage());            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 평점 업데이트 실패.");        }    }    /* 가게 평점을 객체로 가져오기 위해. */    @Getter    private static class RatingDto {        private double rating;    }     /* 좌석 업데이트 (forward처리를 하는 주소.) */    @PatchMapping("/{restaurant}/update-usedSeats")    public void updateFullUsedSeats(@PathVariable(name = "restaurant") String restaurant, @RequestParam("increase") boolean increase,                                                      HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        RestaurantResponseDto restaurantDto = restaurantService.findByName(restaurant);        int value = increase ? 1 : -1;        String forwardUrl;        String key = redisUtil.generateRedisKey(restaurant);        if(value == -1 && redisUtil.getReservationSizeFromRedis(key) != 0) {   // 좌석이 다 차서 예약자에서 인원을 가져올 때. (인원이 줄면) redis값을 가져와 있는지 확인한 후 보내기            forwardUrl = "/restaurant/" + restaurant + "/reservation/decreaseBooking";        }        else {                                                          // 좌석이 덜 찼을 때            forwardUrl = "/restaurant/" + restaurant + "/update-usedSeats/" + value;        }        request.getRequestDispatcher(forwardUrl).forward(request, response);        // forward는 redirect와는 달리 클라이언트에 보여지는게 아님.    }     /* 좌석 업데이트 */    @PatchMapping("/{restaurant}/update-usedSeats/{value}")    public ResponseEntity<String> updateUsedSeats(@PathVariable(name = "restaurant") String restaurant, @PathVariable(name = "value") int value) {        RestaurantResponseDto restaurantDto = restaurantService.findByName(restaurant);        if(value == 1 && restaurantDto.getUsedSeats() < restaurantDto.getTotalSeats()) {           restaurantService.updateUsedSeats(restaurant, value);            return ResponseEntity.ok("가게 좌석 증가 완료.");        } else if (value == -1) {            restaurantService.updateUsedSeats(restaurant, value);            return ResponseEntity.ok("가게 좌석 감소 완료.");        } else {            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 좌석 업데이트 실패.");        }    }}