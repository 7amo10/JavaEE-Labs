package com.ee.lab.security.service;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.security.enterprise.CallerPrincipal;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SimpleSecurityContext {

    private final Principal callerPrincipal;
    private final Set<String> callerRoles;

    public SimpleSecurityContext(CredentialValidationResult validationResult) {
        if (validationResult != null && validationResult.getStatus() == CredentialValidationResult.Status.VALID) {
            this.callerPrincipal = validationResult.getCallerPrincipal();
            this.callerRoles = Collections.unmodifiableSet(new HashSet<>(validationResult.getCallerGroups()));
        } else {
            this.callerPrincipal = null;
            this.callerRoles = Collections.emptySet();
        }
    }

    public Principal getCallerPrincipal() {
        return callerPrincipal;
    }

    public boolean isAuthenticated() {
        return callerPrincipal != null;
    }

    public boolean isCallerInRole(String role) {
        return callerRoles.contains(role);
    }

    public Set<String> getCallerRoles() {
        return callerRoles;
    }

    /**
     * Inspects method security annotations (@PermitAll, @DenyAll, @RolesAllowed)
     * and evaluates access for the current security context.
     */
    public boolean hasAccessTo(Class<?> targetClass, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = targetClass.getMethod(methodName, parameterTypes);

            // 1. @DenyAll always rejects access
            if (method.isAnnotationPresent(DenyAll.class)) {
                return false;
            }

            // 2. @PermitAll allows everyone (even unauthenticated)
            if (method.isAnnotationPresent(PermitAll.class)) {
                return true;
            }

            // 3. @RolesAllowed requires authenticated caller possessing at least one matching role
            if (method.isAnnotationPresent(RolesAllowed.class)) {
                if (!isAuthenticated()) {
                    return false;
                }
                RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
                for (String requiredRole : rolesAllowed.value()) {
                    if (isCallerInRole(requiredRole)) {
                        return true;
                    }
                }
                return false;
            }

            // Default to permit if no annotations present
            return true;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Method not found: " + methodName, e);
        }
    }
}
