package com.riskguard.transaction.api;
import com.riskguard.transaction.api.dto.ReviewDtos; import com.riskguard.transaction.service.FraudReviewService; import jakarta.validation.Valid; import java.util.UUID; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/analyst/transactions") public class ReviewController { private final FraudReviewService service; public ReviewController(FraudReviewService service){this.service=service;}
 @PostMapping("/{transactionId}/reviews") public ReviewDtos.Response review(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID transactionId,@RequestHeader(value="X-Correlation-Id",required=false) String correlationId,@Valid @RequestBody ReviewDtos.Create body){return service.review(UUID.fromString(jwt.getSubject()),transactionId,body,correlationId==null?UUID.randomUUID().toString():correlationId);}
 @GetMapping("/{transactionId}/reviews") public java.util.List<ReviewDtos.Response> list(@PathVariable UUID transactionId){return service.list(transactionId);}
}
