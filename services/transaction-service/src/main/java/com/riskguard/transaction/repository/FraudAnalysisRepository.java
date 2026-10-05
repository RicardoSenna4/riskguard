package com.riskguard.transaction.repository;
import com.riskguard.transaction.domain.FraudAnalysis; import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis,UUID> { Optional<FraudAnalysis> findByTransactionId(UUID transactionId); boolean existsByEventId(UUID eventId); }
