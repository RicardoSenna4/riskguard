package com.riskguard.transaction.repository;

import com.riskguard.transaction.domain.Customer;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findAllByUserId(UUID userId, Pageable pageable);
    boolean existsByDocument(String document);
}
