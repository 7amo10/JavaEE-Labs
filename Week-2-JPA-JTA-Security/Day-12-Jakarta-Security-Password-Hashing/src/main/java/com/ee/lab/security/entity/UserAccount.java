package com.ee.lab.security.entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserAccount {

    private final String username;
    private final String passwordHash;
    private final Set<String> roles;
    private final boolean enabled;

    public UserAccount(String username, String passwordHash, Set<String> roles, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
