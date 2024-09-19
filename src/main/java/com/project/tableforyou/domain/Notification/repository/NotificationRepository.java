package com.project.tableforyou.domain.Notification.repository;

import com.project.tableforyou.domain.Notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUser_Id(Long userId, Pageable pageable);
}
