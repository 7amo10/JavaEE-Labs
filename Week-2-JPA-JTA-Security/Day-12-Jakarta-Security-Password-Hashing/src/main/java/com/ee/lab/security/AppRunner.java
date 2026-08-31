package com.ee.lab.security;

import com.ee.lab.security.hashing.DefaultPbkdf2PasswordHash;
import com.ee.lab.security.identitystore.InMemoryIdentityStore;
import com.ee.lab.security.service.ClusterControlService;
import com.ee.lab.security.service.SimpleSecurityContext;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 DAY 12: JAKARTA SECURITY 3.0 & PBKDF2 PASSWORD HASHING");
        System.out.println("   IdentityStore | Pbkdf2PasswordHash | @RolesAllowed | RBAC Authorization");
        System.out.println("================================================================================");

        Pbkdf2PasswordHash passwordHasher = new DefaultPbkdf2PasswordHash();
        InMemoryIdentityStore identityStore = new InMemoryIdentityStore(passwordHasher);
        ClusterControlService clusterService = new ClusterControlService();

        // --------------------------------------------------------------------------------
        // SCENARIO 1: PBKDF2 CRYPTOGRAPHIC HASHING & SALT VERIFICATION
        // --------------------------------------------------------------------------------
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" SCENARIO 1: PBKDF2 HASH GENERATION & CRYPTOGRAPHIC SALT VERIFICATION");
        System.out.println("--------------------------------------------------------------------------------");
        String rawPassword = "ClusterSuperKey#2026";
        String hash1 = passwordHasher.generate(rawPassword.toCharArray());
        String hash2 = passwordHasher.generate(rawPassword.toCharArray());

        System.out.println(" [RAW PASSWORD] '" + rawPassword + "'");
        System.out.println(" [PBKDF2 HASH 1] " + hash1);
        System.out.println(" [PBKDF2 HASH 2] " + hash2);

        boolean verifyMatch1 = passwordHasher.verify(rawPassword.toCharArray(), hash1);
        boolean verifyMatch2 = passwordHasher.verify(rawPassword.toCharArray(), hash2);
        boolean verifyMismatch = passwordHasher.verify("BadPassword123".toCharArray(), hash1);

        System.out.println(" [SALT ISOLATION] Hash 1 matches Hash 2 literally? " + hash1.equals(hash2) + " (Unique dynamic salt per hash)");
        System.out.println(" [VERIFICATION 1] Hash 1 verified with correct password? " + verifyMatch1);
        System.out.println(" [VERIFICATION 2] Hash 2 verified with correct password? " + verifyMatch2);
        System.out.println(" [VERIFICATION 3] Hash 1 rejected with incorrect password? " + (!verifyMismatch));

        // --------------------------------------------------------------------------------
        // SCENARIO 2: IDENTITYSTORE AUTHENTICATION SUCCESS (ADMIN USER)
        // --------------------------------------------------------------------------------
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" SCENARIO 2: IDENTITYSTORE CREDENTIAL VALIDATION (SUCCESSFUL AUTHENTICATION)");
        System.out.println("--------------------------------------------------------------------------------");
        CredentialValidationResult adminResult = identityStore.validate(
                new UsernamePasswordCredential("admin_master", "AdminSecret#2026")
        );

        System.out.println(" [VALIDATION STATUS] " + adminResult.getStatus());
        System.out.println(" [CALLER PRINCIPAL]  " + adminResult.getCallerPrincipal().getName());
        System.out.println(" [ASSIGNED ROLES]    " + adminResult.getCallerGroups());

        // --------------------------------------------------------------------------------
        // SCENARIO 3: IDENTITYSTORE AUTHENTICATION REJECTION (INVALID PASSWORD)
        // --------------------------------------------------------------------------------
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" SCENARIO 3: IDENTITYSTORE REJECTION (BAD PASSWORD)");
        System.out.println("--------------------------------------------------------------------------------");
        CredentialValidationResult badPassResult = identityStore.validate(
                new UsernamePasswordCredential("operator_bob", "WrongPassword!")
        );

        System.out.println(" [VALIDATION STATUS] " + badPassResult.getStatus() + " (Rejected bad credentials)");

        // --------------------------------------------------------------------------------
        // SCENARIO 4: IDENTITYSTORE DELEGATION & LOCKED ACCOUNT HANDLING
        // --------------------------------------------------------------------------------
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" SCENARIO 4: UNKNOWN IDENTITY DELEGATION & DISABLED ACCOUNT POLICY");
        System.out.println("--------------------------------------------------------------------------------");
        CredentialValidationResult unknownResult = identityStore.validate(
                new UsernamePasswordCredential("unknown_intruder", "SomePass#123")
        );
        System.out.println(" [UNKNOWN USER STATUS]  " + unknownResult.getStatus() + " (NOT_VALIDATED allows store chaining)");

        CredentialValidationResult disabledResult = identityStore.validate(
                new UsernamePasswordCredential("disabled_dave", "LockedPass#789")
        );
        System.out.println(" [DISABLED USER STATUS] " + disabledResult.getStatus() + " (INVALID for locked accounts)");

        // --------------------------------------------------------------------------------
        // SCENARIO 5: ROLE-BASED ACCESS CONTROL (RBAC) AUTHORIZATION MATRIX
        // --------------------------------------------------------------------------------
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" SCENARIO 5: DECLARATIVE RBAC ENFORCEMENT (@RolesAllowed, @PermitAll, @DenyAll)");
        System.out.println("--------------------------------------------------------------------------------");

        CredentialValidationResult operatorResult = identityStore.validate(
                new UsernamePasswordCredential("operator_bob", "OperatorPass#123")
        );
        CredentialValidationResult viewerResult = identityStore.validate(
                new UsernamePasswordCredential("viewer_alice", "ViewerPass#456")
        );

        SimpleSecurityContext adminContext = new SimpleSecurityContext(adminResult);
        SimpleSecurityContext operatorContext = new SimpleSecurityContext(operatorResult);
        SimpleSecurityContext viewerContext = new SimpleSecurityContext(viewerResult);
        SimpleSecurityContext anonContext = new SimpleSecurityContext(null);

        evaluateAccess("rebootClusterNode (ADMIN)", ClusterControlService.class, "rebootClusterNode", String.class,
                adminContext, operatorContext, viewerContext, anonContext);

        evaluateAccess("scaleWorkerThreads (ADMIN, OPERATOR)", ClusterControlService.class, "scaleWorkerThreads", new Class<?>[]{String.class, int.class},
                adminContext, operatorContext, viewerContext, anonContext);

        evaluateAccess("viewTelemetryDashboard (ADMIN, OPERATOR, VIEWER)", ClusterControlService.class, "viewTelemetryDashboard", String.class,
                adminContext, operatorContext, viewerContext, anonContext);

        evaluateAccess("getPublicSystemStatus (@PermitAll)", ClusterControlService.class, "getPublicSystemStatus", new Class<?>[]{},
                adminContext, operatorContext, viewerContext, anonContext);

        evaluateAccess("emergencyFactoryReset (@DenyAll)", ClusterControlService.class, "emergencyFactoryReset", new Class<?>[]{},
                adminContext, operatorContext, viewerContext, anonContext);

        System.out.println("\n================================================================================");
        System.out.println("          JAKARTA SECURITY 3.0 LAB COMPLETED SUCCESSFULLY");
        System.out.println("================================================================================");
    }

    private static void evaluateAccess(String operationLabel, Class<?> targetClass, String methodName, Object paramTypes,
                                       SimpleSecurityContext admin, SimpleSecurityContext operator,
                                       SimpleSecurityContext viewer, SimpleSecurityContext anon) {
        Class<?>[] types = (paramTypes instanceof Class<?>[]) ? (Class<?>[]) paramTypes : new Class<?>[]{(Class<?>) paramTypes};

        boolean adminAccess = admin.hasAccessTo(targetClass, methodName, types);
        boolean operatorAccess = operator.hasAccessTo(targetClass, methodName, types);
        boolean viewerAccess = viewer.hasAccessTo(targetClass, methodName, types);
        boolean anonAccess = anon.hasAccessTo(targetClass, methodName, types);

        System.out.println(" -> Operation: " + operationLabel);
        System.out.println("      Admin: " + (adminAccess ? "GRANTED [✓]" : "DENIED [✗]")
                + " | Operator: " + (operatorAccess ? "GRANTED [✓]" : "DENIED [✗]")
                + " | Viewer: " + (viewerAccess ? "GRANTED [✓]" : "DENIED [✗]")
                + " | Anonymous: " + (anonAccess ? "GRANTED [✓]" : "DENIED [✗]"));
    }
}
