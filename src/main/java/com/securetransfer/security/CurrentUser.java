package com.securetransfer.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.stream.Collectors;

/** The authenticated caller, taken from the validated JWT. Never from a request body. */
public record CurrentUser(Long userId, String username, Set<String> authorities) {

    public static CurrentUser from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Expected JWT authentication");
        }
        Set<String> authorities = jwtAuth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentUser(
                Long.valueOf(jwtAuth.getToken().getSubject()),
                jwtAuth.getToken().getClaimAsString(TokenClaims.USERNAME),
                authorities);
    }

    public boolean hasRole(String role) {
        return authorities.contains("ROLE_" + role);
    }

    public boolean hasPermission(PermissionCode permission) {
        return authorities.contains(permission.name());
    }
}
