package com.riskguard.transaction.repository;
import com.riskguard.transaction.domain.AuditEvent; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}
