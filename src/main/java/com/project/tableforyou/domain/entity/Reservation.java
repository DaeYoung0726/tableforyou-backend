package com.project.tableforyou.domain.entity;


import com.project.tableforyou.domain.dto.ReservationDto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {


    // 최대 예약 건수 설정을 해야할 듯.
    private int booking;

    private String username;

    private String restaurant;

    public Reservation(ReservationDto.Response dto) {
        this.booking = dto.getBooking();
    }

    public void update(int booking) {
        this.booking = booking;
    }
}
