package com.riskguard.transaction.repository;
import com.riskguard.transaction.domain.FraudReview; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface FraudReviewRepository extends JpaRepository<FraudReview, UUID> { List<FraudReview> findAllByTransactionIdOrderByReviewedAtAsc(UUID transactionId); }
