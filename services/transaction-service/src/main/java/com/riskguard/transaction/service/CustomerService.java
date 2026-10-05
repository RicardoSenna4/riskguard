package com.riskguard.transaction.service;

import com.riskguard.transaction.api.dto.CustomerDtos;
import com.riskguard.transaction.api.dto.PageResponse;
import com.riskguard.transaction.domain.Customer;
import com.riskguard.transaction.domain.UserAccount;
import com.riskguard.transaction.repository.CustomerRepository;
import com.riskguard.transaction.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private final CustomerRepository customers; private final UserRepository users; private final AuditService audit;
    public CustomerService(CustomerRepository customers, UserRepository users, AuditService audit) { this.customers = customers; this.users = users; this.audit = audit; }
    @Transactional
    public CustomerDtos.Response create(UUID userId, CustomerDtos.Create request) {
        String document = normalize(request.document());
        if (customers.existsByDocument(document)) throw new DomainExceptions.Conflict("Document is already registered");
        UserAccount user = users.findById(userId).orElseThrow(() -> new DomainExceptions.NotFound("User not found"));
        Customer saved = customers.save(new Customer(UUID.randomUUID(), user, document, normalize(request.name()), normalize(request.email()), Instant.now()));
        return toResponse(saved);
    }
    @Transactional(readOnly = true)
    public CustomerDtos.Response get(UUID userId, UUID id, boolean elevated) { return toResponse(find(userId, id, elevated)); }
    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.Response> list(UUID userId, boolean elevated, Pageable pageable) {
        Page<Customer> page = elevated ? customers.findAll(pageable) : customers.findAllByUserId(userId, pageable);
        return page(page);
    }
    @Transactional
    public CustomerDtos.Response update(UUID userId, UUID id, CustomerDtos.Update request, boolean elevated) {
        Customer customer = find(userId, id, elevated);
        customer.update(normalize(request.name()), normalize(request.email()), Instant.now());
        return toResponse(customer);
    }
    @Transactional
    public CustomerDtos.Response setActive(UUID userId, UUID id, boolean active, boolean elevated) {
        Customer customer = find(userId, id, elevated);
        boolean before = customer.isActive(); if (active) customer.activate(Instant.now()); else customer.deactivate(Instant.now());
        audit.record("CUSTOMER_STATUS_CHANGED", userId, UUID.randomUUID().toString(), "CUSTOMER", id, java.util.Map.of("active", before), java.util.Map.of("active", active));
        return toResponse(customer);
    }
    private Customer find(UUID userId, UUID id, boolean elevated) {
        Customer customer = customers.findById(id).orElseThrow(() -> new DomainExceptions.NotFound("Customer not found"));
        if (!elevated && !customer.getUser().getId().equals(userId)) throw new DomainExceptions.ApiException(HttpStatus.FORBIDDEN, "You do not have access to this customer");
        return customer;
    }
    private static String normalize(String value) { return value.trim(); }
    private static CustomerDtos.Response toResponse(Customer c) { return new CustomerDtos.Response(c.getId(), c.getUser().getId(), c.getDocument(), c.getName(), c.getEmail(), c.isActive(), c.getVersion(), c.getCreatedAt(), c.getUpdatedAt()); }
    private static PageResponse<CustomerDtos.Response> page(Page<Customer> p) { return new PageResponse<>(p.getContent().stream().map(CustomerService::toResponse).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast()); }
}
