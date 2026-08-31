package com.ee.lab.security.identitystore;

import com.ee.lab.security.entity.UserAccount;
import com.ee.lab.security.hashing.DefaultPbkdf2PasswordHash;
import jakarta.security.enterprise.CallerPrincipal;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;

import java.util.*;

public class InMemoryIdentityStore implements IdentityStore {

    private final Map<String, UserAccount> users = new HashMap<>();
    private final Pbkdf2PasswordHash passwordHash;

    public InMemoryIdentityStore() {
        this.passwordHash = new DefaultPbkdf2PasswordHash();
        initDefaultUsers();
    }

    public InMemoryIdentityStore(Pbkdf2PasswordHash passwordHash) {
        this.passwordHash = passwordHash;
        initDefaultUsers();
    }

    private void initDefaultUsers() {
        // Admin: has ADMIN and OPERATOR roles
        registerUser("admin_master", "AdminSecret#2026", Set.of("ADMIN", "OPERATOR", "VIEWER"), true);

        // Operator: has OPERATOR and VIEWER roles
        registerUser("operator_bob", "OperatorPass#123", Set.of("OPERATOR", "VIEWER"), true);

        // Viewer: read-only VIEWER role
        registerUser("viewer_alice", "ViewerPass#456", Set.of("VIEWER"), true);

        // Disabled User: account locked
        registerUser("disabled_dave", "LockedPass#789", Set.of("VIEWER"), false);
    }

    public void registerUser(String username, String rawPassword, Set<String> roles, boolean enabled) {
        String hash = passwordHash.generate(rawPassword.toCharArray());
        users.put(username, new UserAccount(username, hash, roles, enabled));
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (!(credential instanceof UsernamePasswordCredential userPass)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        String username = userPass.getCaller();
        String rawPassword = userPass.getPasswordAsString();

        UserAccount account = users.get(username);
        if (account == null) {
            // User not found in this store -> return NOT_VALIDATED to allow chaining
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        if (!account.isEnabled()) {
            // Account is disabled -> rejection
            return CredentialValidationResult.INVALID_RESULT;
        }

        boolean isValid = passwordHash.verify(rawPassword.toCharArray(), account.getPasswordHash());
        if (isValid) {
            return new CredentialValidationResult(
                    new CallerPrincipal(account.getUsername()),
                    account.getRoles()
            );
        } else {
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    public UserAccount getAccount(String username) {
        return users.get(username);
    }
}
