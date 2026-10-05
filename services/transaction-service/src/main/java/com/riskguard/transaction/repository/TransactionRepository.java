package com.riskguard.transaction.repository;

import com.riskguard.transaction.domain.RiskLevel;
import com.riskguard.transaction.domain.Transaction;
import com.riskguard.transaction.domain.TransactionStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findAllByCustomerUserId(UUID userId, Pageable pageable);
    Page<Transaction> findAllByCustomerUserIdAndCustomerId(UUID userId, UUID customerId, Pageable pageable);
    Page<Transaction> findAllByCustomerUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(UUID userId, Instant from, Instant to, Pageable pageable);
    Page<Transaction> findAllByCustomerUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);
    Page<Transaction> findAllByCustomerUserIdAndRisk(UUID userId, RiskLevel risk, Pageable pageable);
    Page<Transaction> findAllByCustomerUserIdAndMerchantContainingIgnoreCase(UUID userId, String merchant, Pageable pageable);
    Page<Transaction> findAllByCustomerId(UUID customerId, Pageable pageable);
    boolean existsByExternalId(String externalId);
    @Query("select t from Transaction t where (:customerId is null or t.customer.id = :customerId) and (:from is null or t.createdAt >= :from) and (:to is null or t.createdAt < :to) and (:status is null or t.status = :status) and (:risk is null or t.risk = :risk) and (:merchant is null or lower(t.merchant) like lower(concat('%', :merchant, '%'))) order by t.createdAt desc")
    Page<Transaction> searchAll(UUID customerId, java.time.Instant from, java.time.Instant to, TransactionStatus status, RiskLevel risk, String merchant, Pageable pageable);
    @Query("select t from Transaction t where t.customer.user.id = :userId and (:customerId is null or t.customer.id = :customerId) and (:from is null or t.createdAt >= :from) and (:to is null or t.createdAt < :to) and (:status is null or t.status = :status) and (:risk is null or t.risk = :risk) and (:merchant is null or lower(t.merchant) like lower(concat('%', :merchant, '%'))) order by t.createdAt desc")
    Page<Transaction> search(UUID userId, UUID customerId, java.time.Instant from, java.time.Instant to, TransactionStatus status, RiskLevel risk, String merchant, Pageable pageable);
}
