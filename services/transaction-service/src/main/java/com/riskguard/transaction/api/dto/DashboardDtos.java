package com.riskguard.transaction.api.dto;
import java.math.BigDecimal; import java.util.List;
public final class DashboardDtos { private DashboardDtos(){}
 public record Summary(long totalTransactions,long approved,long review,long blocked,BigDecimal averageRiskScore,BigDecimal fraudRate, List<StatusCount> volumeByStatus,List<ReasonCount> topReasons,List<MerchantCount> topMerchants){}
 public record StatusCount(String status,long count){}
 public record ReasonCount(String reason,long count){}
 public record MerchantCount(String merchant,long count){}
}
