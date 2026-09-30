package com.riskguard.transaction.service;

import com.riskguard.transaction.domain.UserAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    public JwtTokenService(JwtEncoder encoder) { this.encoder = encoder; }
    public String issue(UserAccount user, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("riskguard").subject(user.getId().toString())
            .issuedAt(issuedAt).expiresAt(expiresAt).claim("email", user.getEmail())
            .claim("roles", List.of("ROLE_" + user.getRole().name())).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
