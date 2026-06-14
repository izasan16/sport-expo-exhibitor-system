package com.sportexpo.exhibitor.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    long countByStatus(String status);

    List<Payment> findTop5ByStatusOrderByCreatedAtDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Query("select p.orderId from Payment p where p.id = :paymentId")
    Optional<UUID> findOrderIdByPaymentId(@Param("paymentId") UUID paymentId);

    List<Payment> findByOrderIdAndStatus(UUID orderId, String status);

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByStatus(String status);
}
