package com.project.tableforyou.domain.restaurant.service;

import com.project.tableforyou.domain.restaurant.dto.RestaurantResponseDto;
import com.project.tableforyou.domain.restaurant.entity.Region;
import com.project.tableforyou.domain.restaurant.entity.Restaurant;
import com.project.tableforyou.domain.restaurant.entity.RestaurantStatus;
import com.project.tableforyou.domain.restaurant.repository.RestaurantRepository;
import com.project.tableforyou.handler.exceptionHandler.error.ErrorCode;
import com.project.tableforyou.handler.exceptionHandler.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    /* 가게 읽기 */
    @Transactional(readOnly = true)
    public RestaurantResponseDto readRestaurant(Long restaurantId) {

        log.info("Finding restaurant by name: {}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() ->
                new CustomException(ErrorCode.RESTAURANT_NOT_FOUND));
        return new RestaurantResponseDto(restaurant);
    }

    /* 가게 리스트 페이징. 등록된 가게만 들고오기 */
    @Transactional(readOnly = true)
    public Page<RestaurantResponseDto> restaurantPageList(Pageable pageable) {

        log.info("Finding all restaurants");

        Page<Restaurant> restaurants = restaurantRepository.findByStatus(RestaurantStatus.APPROVED, pageable);
        return restaurants.map(RestaurantResponseDto::new);
    }

    /* 지역별 가게 불러오기 */
    @Transactional(readOnly = true)
    public Page<RestaurantResponseDto> restaurantPageListByRegion(String region, Pageable pageable) {

        log.info("Finding by region");

        Page<Restaurant> restaurants =
                restaurantRepository.findByRegionAndStatus(Region.valueOf(region), RestaurantStatus.APPROVED, pageable);
        return restaurants.map(RestaurantResponseDto::new);
    }

    /* 주소로 가게 불러오기 */
    @Transactional(readOnly = true)
    public Page<RestaurantResponseDto> restaurantPageListByLocation(String searchKeyword, Pageable pageable) {

        log.info("Finding all restaurants with searchKeyword: {}", searchKeyword);
        Page<Restaurant> restaurants =
                restaurantRepository.findByLocationContainingAndStatus(searchKeyword, RestaurantStatus.APPROVED, pageable);
        return restaurants.map(RestaurantResponseDto::new);
    }

    /* 가게 검색 || 가게 소개 검색 페이징 */
    @Transactional(readOnly = true)
    public Page<RestaurantResponseDto> restaurantPageSearchList(String searchKeyword, Pageable pageable) {

        log.info("Finding all restaurants with searchKeyword: {}", searchKeyword);
        Page<Restaurant> restaurants = restaurantRepository.
                findByStatusAndNameContainingOrDescriptionContaining(RestaurantStatus.APPROVED, searchKeyword, searchKeyword, pageable);
        return restaurants.map(RestaurantResponseDto::new);
    }

    /* 가게 좌석 업데이트 */
    @Transactional
    public void updateUsedSeats(Long restaurantId, int value) {    // 가게에 user를 추가해야 하지 않나? 그리고 인원이 줄면 어떻게 user을 없애지? 그리고 예약자를 줄이고 여기로 다시 보내야하는데
        restaurantRepository.updateUsedSeats(restaurantId, value);
        log.info("Restaurant usedSeat updated successfully with restaurant: {}", restaurantId);
    }

    /* 평점 업데이트 */
    @Transactional
    public void updateRating(Long restaurantId, double rating) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId).orElseThrow(() ->
                new CustomException(ErrorCode.RESTAURANT_NOT_FOUND));
        double before_rating = restaurant.getRating();
        int nowRatingNum = restaurant.getRatingNum() + 1;

        double nowRating = 0.0;
        if(nowRatingNum == 1)
            nowRating = rating;
        else
            nowRating = before_rating + (rating - before_rating) / nowRatingNum;  // 누적 평균 공식.


        restaurant.updateRating(nowRating, nowRatingNum);
        log.info("Restaurant rating updated successfully with name: {}", restaurantId);
    }
}