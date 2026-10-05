package com.riskguard.transaction.api.dto;
import java.util.List;
public final class TransactionDetailsDtos { private TransactionDetailsDtos(){} public record Response(TransactionDtos.Response transaction,List<String> reasons,String correlationId,List<TransactionDtos.HistoryResponse> timeline,List<ReviewDtos.Response> reviews){} }
