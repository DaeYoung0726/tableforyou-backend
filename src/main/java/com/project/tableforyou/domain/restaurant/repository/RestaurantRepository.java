package com.project.tableforyou.domain.restaurant.repository;

import com.project.tableforyou.domain.restaurant.entity.Restaurant;
import com.project.tableforyou.domain.restaurant.entity.RestaurantStatus;
import com.project.tableforyou.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Page<Restaurant> findByStatusAndNameContainingOrDescriptionContaining(RestaurantStatus status, String searchKeyword1, String searchKeyword2, Pageable pageable);
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);

    @Modifying
    @Query("update Restaurant r set r.usedSeats = r.usedSeats + :value where r.id = :id")
    void updateUsedSeats(@Param("id") Long id, @Param("value") int value); // JPQL의 id와 매핑하기 위해.

}
