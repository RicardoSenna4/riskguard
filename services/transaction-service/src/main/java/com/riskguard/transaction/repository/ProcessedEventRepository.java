package com.riskguard.transaction.repository;
import com.riskguard.transaction.domain.ProcessedEvent; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent,UUID> {}
