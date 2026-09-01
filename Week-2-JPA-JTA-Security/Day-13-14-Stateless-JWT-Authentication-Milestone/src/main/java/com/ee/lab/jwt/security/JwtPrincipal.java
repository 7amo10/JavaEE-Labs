package com.ee.lab.jwt.security;

import java.security.Principal;

public class JwtPrincipal implements Principal {

    private final String name;
    private final JwtClaims claims;

    public JwtPrincipal(String name, JwtClaims claims) {
        this.name = name;
        this.claims = claims;
    }

    @Override
    public String getName() {
        return name;
    }

    public JwtClaims getClaims() {
        return claims;
    }
}
