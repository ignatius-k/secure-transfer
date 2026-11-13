package com.securetransfer.auth;

import com.securetransfer.error.InvalidCredentialsException;
import com.securetransfer.security.Permission;
import com.securetransfer.security.Role;
import com.securetransfer.security.TokenClaims;
import com.securetransfer.security.User;
import com.securetransfer.security.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Verifies credentials and issues a short-lived RS256 JWT carrying the user's roles and
 * (non-deprecated) permissions.
 */
@Service
public class TokenService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;
    /** Used to spend the same BCrypt time for unknown users, so response timing doesn't reveal usernames. */
    private final String dummyHash;

    public TokenService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtEncoder jwtEncoder,
                        Clock clock,
                        @Value("${app.security.jwt.issuer}") String issuer,
                        @Value("${app.security.jwt.access-token-ttl}") Duration ttl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse issueToken(TokenRequest request) {
        Optional<User> found = userRepository.findByUsername(request.username());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .filter(p -> !p.isDeprecated())
                .map(Permission::getCode)
                .distinct()
                .sorted()
                .toList();

        Instant now = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(TokenClaims.USERNAME, user.getUsername())
                .claim(TokenClaims.ROLES, roles)
                .claim(TokenClaims.PERMISSIONS, permissions)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }
}
