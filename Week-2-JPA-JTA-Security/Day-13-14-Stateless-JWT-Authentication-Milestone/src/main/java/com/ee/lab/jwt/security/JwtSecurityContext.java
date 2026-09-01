package com.ee.lab.jwt.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

public class JwtSecurityContext implements SecurityContext {

    private final JwtPrincipal principal;
    private final boolean secure;

    public JwtSecurityContext(JwtPrincipal principal, boolean secure) {
        this.principal = principal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (principal == null || principal.getClaims() == null) {
            return false;
        }
        return principal.getClaims().getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "BEARER";
    }
}
