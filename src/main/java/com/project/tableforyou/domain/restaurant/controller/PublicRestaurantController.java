package com.project.tableforyou.domain.restaurant.controller;import com.project.tableforyou.domain.reservation.service.ReservationService;import com.project.tableforyou.domain.restaurant.dto.RestaurantInfoDto;import com.project.tableforyou.domain.restaurant.dto.RestaurantResponseDto;import com.project.tableforyou.domain.restaurant.service.RestaurantService;import com.project.tableforyou.handler.exceptionHandler.error.ErrorCode;import com.project.tableforyou.handler.exceptionHandler.exception.CustomException;import com.project.tableforyou.utils.redis.RedisUtil;import jakarta.servlet.ServletException;import jakarta.servlet.http.HttpServletRequest;import jakarta.servlet.http.HttpServletResponse;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;import org.springframework.data.domain.Page;import org.springframework.data.domain.PageRequest;import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Sort;import org.springframework.data.web.PageableDefault;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;import java.io.IOException;import static com.project.tableforyou.utils.redis.RedisProperties.RESERVATION_KEY_PREFIX;@RestController@RequestMapping("/public/restaurants")@RequiredArgsConstructor@Slf4jpublic class PublicRestaurantController {    private final RestaurantService restaurantService;    private final ReservationService reservationService;    private final RedisUtil redisUtil;    /* 가게 불러오기 */    @GetMapping("/{restaurantId}")    public RestaurantResponseDto read(@PathVariable(name = "restaurantId") Long restaurantId) {        return restaurantService.readRestaurant(restaurantId);    }    /* 전체 가게 불러오기. 페이징 처리 + 검색 기능 */    @GetMapping    public Page<RestaurantInfoDto> readAll(            @PageableDefault(size = 20, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable,            @RequestParam(required = false, value = "type") String type,            @RequestParam(required = false, value = "search-keyword") String searchKeyword,            @RequestParam(required = false, value = "sort-by", defaultValue = "rating") String sortBy,            @RequestParam(required = false, value = "direction", defaultValue = "DESC") String direction) {        // rating가 아닌 다른 정렬 방식 선택        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);        if (type == null) {            return restaurantService.restaurantPageList(sortedPageable);        }        return switch (type) {            case "restaurant" -> restaurantService.restaurantPageSearchList(searchKeyword, sortedPageable);            case "region" -> restaurantService.restaurantPageListByRegion(searchKeyword, sortedPageable);            case "location" -> restaurantService.restaurantPageListByLocation(searchKeyword, sortedPageable);            default -> throw new CustomException(ErrorCode.INVALID_PARAMETER);        };    }    /* 가게 예약자 수 불러오기 */    @GetMapping("/{restaurantId}/waiting")    public int waiting(@PathVariable(name = "restaurantId") Long restaurantId) {        return reservationService.RestaurantWaiting(restaurantId);    }    /* 좌석 업데이트 (forward처리를 하는 주소.) */    @PatchMapping("/{restaurantId}/update-used-seats")    public void updateFullUsedSeats(@PathVariable(name = "restaurantId") Long restaurantId,                                    @RequestParam("increase") boolean increase,                                    HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {        int value = increase ? 1 : -1;        String forwardUrl;        String key = RESERVATION_KEY_PREFIX + restaurantId;        if(value == -1 && redisUtil.hashSize(key) != 0) {   // 좌석이 다 차서 예약자에서 인원을 가져올 때. (인원이 줄면) redis값을 가져와 있는지 확인한 후 보내기            forwardUrl = "/public/restaurants/" + restaurantId + "/reservations/decrease-booking";        }        else {                                                          // 좌석이 덜 찼을 때            forwardUrl = "/public/restaurants/" + restaurantId + "/update-used-seats/" + value;        }        request.getRequestDispatcher(forwardUrl).forward(request, response);        // forward는 redirect와는 달리 클라이언트에 보여지는게 아님.    }    /* 좌석 업데이트 */    @PatchMapping("/{restaurantId}/update-used-seats/{value}")    public ResponseEntity<String> updateUsedSeats(@PathVariable(name = "restaurantId") Long restaurantId,                                                  @PathVariable(name = "value") int value) {        RestaurantResponseDto restaurantDto = restaurantService.readRestaurant(restaurantId);        if(value == 1 && restaurantDto.getUsedSeats() < restaurantDto.getTotalSeats()) {            restaurantService.updateUsedSeats(restaurantId, value);            return ResponseEntity.ok("가게 좌석 증가 완료.");        } else if (value == -1) {            restaurantService.updateUsedSeats(restaurantId, value);            return ResponseEntity.ok("가게 좌석 감소 완료.");        } else {            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 좌석 업데이트 실패.");        }    }}