package egi.eu;

import io.smallrye.mutiny.tuples.Tuple2;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestQuery;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.security.identity.SecurityIdentity;
import org.hibernate.reactive.mutiny.Mutiny;
import io.smallrye.mutiny.Uni;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.util.*;

import egi.checkin.model.CheckinUser;
import egi.eu.entity.UserEntity;
import egi.eu.entity.GovernanceEntity;
import egi.eu.model.*;


/***
 * Resource for governance queries and operations.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Governance")
public class TheGovernance extends BaseResource {

    private static final Logger log = Logger.getLogger(TheGovernance.class);

    @Inject
    MeterRegistry registry;

    @Inject
    SecurityIdentity identity;

    @Inject
    IntegratedManagementSystemConfig imsConfig;

    @Inject
    Mutiny.SessionFactory sf;

    // Parameter(s) to add to all endpoints
    @RestHeader(TEST_STUB)
    @Parameter(hidden = true)
    @Schema(defaultValue = "default")
    String stub;


    /***
     * Constructor
     */
    public TheGovernance() { super(log); }

    /**
     * Get governance configuration.
     * @param auth The access token needed to call the service.
     * @param allVersions True to return all versions of the process.
     * @return API Response, wraps a {@link Governance} or an ActionError entity
     */
    @GET
    @Path("/governance")
    @SecurityRequirement(name = "OIDC")
    @RolesAllowed(Role.IMS_USER)
    @Operation(operationId = "getGovernance",  summary = "Get governance details",
               description = "When all versions are requested the field history will hold versions prior " +
                             "to the latest one, sorted by version in descending order.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = Governance.class))),
            @APIResponse(responseCode = "400", description="Invalid parameters or configuration",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ActionError.class))),
            @APIResponse(responseCode = "401", description="Authorization required"),
            @APIResponse(responseCode = "403", description="Permission denied"),
            @APIResponse(responseCode = "503", description="Try again later")
    })
    public Uni<Response> get(@RestHeader(HttpHeaders.AUTHORIZATION) String auth,

                             @RestQuery("allVersions") @DefaultValue("false")
                             @Parameter(required = false, description = "Whether to retrieve all versions")
                             boolean allVersions)
    {
        addToDC("userIdCaller", identity.getAttribute(CheckinUser.ATTR_USERID));
        addToDC("userNameCaller", identity.getAttribute(CheckinUser.ATTR_FULLNAME));
        addToDC("processName", imsConfig.group());
        addToDC("allVersions", allVersions);

        log.info("Getting governance info");

        // If we need just the last version, get it now
        Uni<Response> result = Uni.createFrom().nullItem()

            .chain(unused -> {
                return allVersions ?
                        sf.withSession(session -> GovernanceEntity.getAllVersions()) :
                        sf.withSession(session -> GovernanceEntity.getLastVersionAsList());
            })
            .chain(versions -> {
                // Got a list of versions
                if(!versions.isEmpty())
                    log.info("Got governance info");

                var proc = new Governance(versions);
                return Uni.createFrom().item(Response.ok(proc).build());
            })
            .onFailure().recoverWithItem(e -> {
                log.error("Failed to get governance info");
                return new ActionError(e).toResponse();
            });

        return result;
    }

    /**
     * Update governance configuration.
     * @param auth The access token needed to call the service.
     * @param governance The new governance version, includes details about who is making the change.
     * @return API Response, wraps an ActionSuccess or an ActionError entity
     */
    @PUT
    @Path("/governance")
    @SecurityRequirement(name = "OIDC")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({ Role.IMS_OWNER, Role.IMS_MANAGER })
    @Operation(operationId = "updateGovernance",  summary = "Update governance details")
    @APIResponses(value = {
            @APIResponse(responseCode = "201", description = "Updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ActionSuccess.class))),
            @APIResponse(responseCode = "400", description="Invalid parameters or configuration",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ActionError.class))),
            @APIResponse(responseCode = "401", description="Authorization required"),
            @APIResponse(responseCode = "403", description="Permission denied"),
            @APIResponse(responseCode = "503", description="Try again later")
    })
    public Uni<Response> update(@RestHeader(HttpHeaders.AUTHORIZATION) String auth, Governance governance)
    {
        governance.changeBy = new User(
                (String)identity.getAttribute(CheckinUser.ATTR_USERID),
                (String)identity.getAttribute(CheckinUser.ATTR_FULLNAME),
                (String)identity.getAttribute(CheckinUser.ATTR_EMAIL) );

        addToDC("userIdCaller", governance.changeBy.checkinUserId);
        addToDC("userNameCaller", governance.changeBy.fullName);
        addToDC("processName", imsConfig.group());
        addToDC("governance", governance);

        log.info("Updating governance");

        var latest = new ArrayList<GovernanceEntity>();
        Uni<Response> result = Uni.createFrom().nullItem()

            .chain(unused -> {
                return sf.withTransaction((session, tx) -> { return
                    // Get the latest governance version
                    GovernanceEntity.getLastVersion()
                    .chain(latestGovernance -> {
                        // Got the latest version
                        latest.add(latestGovernance);

                        // Get the users linked to this governance that already exist in the database
                        return UserEntity.findByCheckinUserId(governance.changeBy.checkinUserId);
                    })
                    .chain(existingUser -> {
                        // Got the existing user
                        var users = new HashMap<String, UserEntity>();
                        if(null != existingUser)
                            users.put(existingUser.checkinUserId, existingUser);

                        // Create new governance version
                        var latestGovernance = latest.get(0);
                        var newGovernance = new GovernanceEntity(governance, latestGovernance, users);
                        return session.persist(newGovernance);
                    });
                });
            })
            .chain(unused -> {
                // Update complete, success
                log.info("Updated governance");
                return Uni.createFrom().item(Response.ok(new ActionSuccess("Updated"))
                                                     .status(Response.Status.CREATED).build());
            })
            .onFailure().recoverWithItem(e -> {
                log.error("Failed to update governance");
                return new ActionError(e).toResponse();
            });

        return result;
    }

}
