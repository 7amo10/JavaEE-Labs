package com.ee.lab.audit.boundary;

import com.ee.lab.audit.control.UserAuditRepository;
import com.ee.lab.audit.entity.AuditedUser;
import com.ee.lab.audit.security.SecurityAuditService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditBoundaryResource {

    private static UserAuditRepository repository;
    private static final SecurityAuditService securityService = new SecurityAuditService();

    public static void setRepository(UserAuditRepository repo) {
        repository = repo;
    }

    @GET
    @Path("/public/ping")
    public Response ping() {
        return Response.ok(Map.of("status", "UP", "message", "Audit Public Gateway Active")).build();
    }

    @POST
    @Path("/public/login")
    public Response login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Missing credentials")).build();
        }

        AuditedUser user = repository.findByUsernameSecure(username);
        if (user != null && securityService.verifyPassword(password, user.getPasswordHash())) {
            String token = securityService.createStatelessToken(user.getUsername(), user.getRole());
            return Response.ok(Map.of("token", token, "username", user.getUsername(), "role", user.getRole())).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "Invalid username or password")).build();
    }

    @GET
    @Path("/secured/user/{username}")
    public Response getUserProfile(@PathParam("username") String username) {
        AuditedUser user = repository.findByUsernameSecure(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "logsRecorded", user.getSystemLogs().size()
        )).build();
    }

    @GET
    @Path("/admin/system-overview")
    public Response getAdminOverview() {
        return Response.ok(Map.of(
                "auditStatus", "VERIFIED",
                "securityFilter", "ACTIVE",
                "sqlInjectionProtection", "PARAMETERIZED"
        )).build();
    }
}
