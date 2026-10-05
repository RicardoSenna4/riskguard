package com.riskguard.transaction.service;

import com.riskguard.transaction.api.dto.PageResponse;
import com.riskguard.transaction.api.dto.TransactionDtos;
import com.riskguard.transaction.domain.*;
import com.riskguard.transaction.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository transactions; private final CustomerRepository customers; private final UserRepository users; private final TransactionStatusHistoryRepository history;
    public TransactionService(TransactionRepository transactions, CustomerRepository customers, UserRepository users, TransactionStatusHistoryRepository history) { this.transactions = transactions; this.customers = customers; this.users = users; this.history = history; }
    @Transactional
    public TransactionDtos.Response create(UUID userId, TransactionDtos.Create request, boolean elevated) {
        if (transactions.existsByExternalId(request.externalId().trim())) throw new DomainExceptions.Conflict("External ID is already registered");
        Customer customer = customers.findById(request.customerId()).orElseThrow(() -> new DomainExceptions.NotFound("Customer not found"));
        if (!elevated && !customer.getUser().getId().equals(userId)) throw new DomainExceptions.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "You do not have access to this customer");
        if (!customer.isActive()) throw new DomainExceptions.Conflict("Customer is inactive");
        BigDecimal amount = request.amount().setScale(4);
        if (amount.signum() <= 0) throw new DomainExceptions.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Amount must be positive");
        Instant now = Instant.now(); Transaction transaction = transactions.save(new Transaction(UUID.randomUUID(), customer, request.externalId().trim(), amount, request.currency().trim().toUpperCase(), request.merchant().trim(), now));
        history.save(new TransactionStatusHistory(UUID.randomUUID(), transaction, TransactionStatus.PENDING, now, users.findById(userId).orElse(null)));
        return toResponse(transaction);
    }
    @Transactional(readOnly = true)
    public TransactionDtos.Response get(UUID userId, UUID id, boolean elevated) { return toResponse(find(userId, id, elevated)); }
    @Transactional(readOnly = true)
    public PageResponse<TransactionDtos.Response> list(UUID userId, UUID customerId, Instant from, Instant to, TransactionStatus status, RiskLevel risk, String merchant, boolean elevated, Pageable pageable) {
        Page<Transaction> page;
        if (elevated) page = filtered(customerId, from, to, status, risk, merchant, pageable);
        else page = filteredForUser(userId, customerId, from, to, status, risk, merchant, pageable);
        return new PageResponse<>(page.getContent().stream().map(TransactionService::toResponse).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
    @Transactional
    public TransactionDtos.Response changeStatus(UUID userId, UUID id, TransactionDtos.StatusUpdate request, boolean elevated) {
        Transaction transaction = find(userId, id, elevated);
        if (transaction.getVersion() != request.version()) throw new DomainExceptions.Conflict("Transaction was modified by another request");
        if (transaction.getStatus() == request.status()) return toResponse(transaction);
        Instant now = Instant.now(); transaction.changeStatus(request.status(), now);
        transactions.saveAndFlush(transaction);
        history.save(new TransactionStatusHistory(UUID.randomUUID(), transaction, request.status(), now, users.findById(userId).orElse(null)));
        return toResponse(transaction);
    }
    @Transactional(readOnly = true)
    public java.util.List<TransactionDtos.HistoryResponse> history(UUID userId, UUID id, boolean elevated) { find(userId, id, elevated); return history.findAllByTransactionIdOrderByChangedAtAsc(id).stream().map(h -> new TransactionDtos.HistoryResponse(h.getId(), h.getStatus(), h.getChangedAt(), h.getChangedBy() == null ? null : h.getChangedBy().getId())).toList(); }
    private Transaction find(UUID userId, UUID id, boolean elevated) { Transaction t = transactions.findById(id).orElseThrow(() -> new DomainExceptions.NotFound("Transaction not found")); if (!elevated && !t.getCustomer().getUser().getId().equals(userId)) throw new DomainExceptions.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "You do not have access to this transaction"); return t; }
    private Page<Transaction> filteredForUser(UUID u, UUID c, Instant f, Instant t, TransactionStatus s, RiskLevel r, String m, Pageable p) { return transactions.search(u, c, f, t, s, r, m, p); }
    private Page<Transaction> filtered(UUID c, Instant f, Instant t, TransactionStatus s, RiskLevel r, String m, Pageable p) { return transactions.searchAll(c, f, t, s, r, m, p); }
    private static TransactionDtos.Response toResponse(Transaction t) { return new TransactionDtos.Response(t.getId(), t.getCustomer().getId(), t.getExternalId(), t.getAmount(), t.getCurrency(), t.getMerchant(), t.getStatus(), t.getRisk(), t.getVersion(), t.getCreatedAt(), t.getUpdatedAt()); }
}
