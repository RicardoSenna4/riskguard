package com.riskguard.transaction.repository;

import com.riskguard.transaction.domain.TransactionStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, UUID> {
    List<TransactionStatusHistory> findAllByTransactionIdOrderByChangedAtAsc(UUID transactionId);
}
