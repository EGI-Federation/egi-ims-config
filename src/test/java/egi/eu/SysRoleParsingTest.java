package egi.eu;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import egi.eu.model.Role;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import egi.checkin.model.CheckinUser;


@QuarkusTest
public class SysRoleParsingTest {

    @Inject
    IntegratedManagementSystemConfig imsConfig;

    private static final Logger log = Logger.getLogger(SysRoleParsingTest.class);
    private String prefix;
    private final String postfix = "#aai.egi.eu";
    private String imso, imsm, imsd, strco, opsco;
    private Map<String, String> roleNames = new HashMap<String, String>();
    private CheckinUser userInfo;
    private QuarkusSecurityIdentity.Builder builder;
    private SysRoleCustomization roleCustomization;
    private ObjectMapper mapper = new ObjectMapper();


    @BeforeEach
    public void setup() {
        prefix = "urn:mace:egi.eu:group:" + imsConfig.vo() + ":";
        if(roleNames.isEmpty()) {
            roleNames.putAll(imsConfig.roles());

            imso = roleNames.get("ims-owner").toLowerCase();
            imsm = roleNames.get("ims-manager").toLowerCase();
            imsd = roleNames.get("ims-developer").toLowerCase();
            strco = roleNames.get("strategy-coordinator").toLowerCase();
            opsco = roleNames.get("operations-coordinator").toLowerCase();
        }

        roleCustomization = new SysRoleCustomization();
        roleCustomization.setConfig(imsConfig);

        userInfo = new CheckinUser("e9c37aa0d1cf14c56e560f9f9915da6761f54383badb501a2867bc43581b835c@egi.eu");
        userInfo.addEntitlement("urn:mace:egi.eu:group:vo.access.egi.eu:role=member#aai.egi.eu");
        userInfo.addEntitlement("urn:mace:egi.eu:group:vo.access.egi.eu:role=vm_operator#aai.egi.eu");

        builder = QuarkusSecurityIdentity.builder();
        var principal = new QuarkusPrincipal("test");
        builder.setPrincipal(principal);
    }

    @Test
    @DisplayName("All roles require explicit VO membership")
    public void testNoVoMembership() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "admins:role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imso) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsm) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsd) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), strco) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), opsco) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it does not have any of the roles
                return roles.contains(Role.PROCESS_MEMBER) ||
                       roles.contains(Role.IMS_USER) ||
                       roles.contains(Role.IMS_OWNER) ||
                       roles.contains(Role.IMS_MANAGER) ||
                       roles.contains(Role.IMS_DEVELOPER) ||
                       roles.contains(Role.STRATEGY_COORDINATOR) ||
                       roles.contains(Role.OPERATIONS_COORDINATOR);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(false);
    }

    @Test
    @DisplayName("IMS_USER when VO member")
    public void testVoMembership() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it has the correct role
                return roles.contains(Role.IMS_USER);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(true);
    }

    @Test
    @DisplayName("IMS roles require explicit IMS group membership")
    public void testNoGroupMembership() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imso) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsm) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsd) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), strco) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), opsco) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
                .onItem().transform(id -> id.getRoles())
                .onItem().transform(roles -> {
                    // Check that it does not have any of the roles
                    return roles.contains(Role.PROCESS_MEMBER) ||
                           roles.contains(Role.IMS_OWNER) ||
                           roles.contains(Role.IMS_MANAGER) ||
                           roles.contains(Role.IMS_DEVELOPER) ||
                           roles.contains(Role.STRATEGY_COORDINATOR) ||
                           roles.contains(Role.OPERATIONS_COORDINATOR);
                })
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitItem()
                .assertItem(false);
    }

    @Test
    @DisplayName("IMS_OWNER requires both VO and IMS group membership")
    public void testProcessOwner() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imso) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it has the correct role
                return roles.contains(Role.IMS_OWNER) &&
                       roles.contains(Role.PROCESS_MEMBER) &&
                       roles.contains(Role.IMS_USER);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(true);
    }

    @Test
    @DisplayName("IMS_MANAGER requires both VO and IMS group membership")
    public void testProcessManager() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsm) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it has the correct role
                return roles.contains(Role.IMS_MANAGER) &&
                       roles.contains(Role.PROCESS_MEMBER) &&
                       roles.contains(Role.IMS_USER);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(true);
    }

    @Test
    @DisplayName("IMS_DEVELOPER requires both VO and IMS group membership")
    public void testProcessDeveloper() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), imsd) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
                .onItem().transform(id -> id.getRoles())
                .onItem().transform(roles -> {
                    // Check that it has the correct role
                    return roles.contains(Role.IMS_DEVELOPER) &&
                           roles.contains(Role.PROCESS_MEMBER) &&
                           roles.contains(Role.IMS_USER);
                })
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber
                .awaitItem()
                .assertItem(true);
    }

    @Test
    @DisplayName("STRATEGY_COORDINATOR requires both VO and IMS group membership")
    public void testCatalogManager() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), strco) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it has the correct role
                return roles.contains(Role.STRATEGY_COORDINATOR) &&
                       roles.contains(Role.PROCESS_MEMBER) &&
                       roles.contains(Role.IMS_USER);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(true);
    }

    @Test
    @DisplayName("OPERATIONS_COORDINATOR requires both VO and IMS group membership")
    public void testReportOwner() {
        // Setup entitlements
        userInfo.addEntitlement(prefix + "role=member" + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=member", imsConfig.group()) + postfix);
        userInfo.addEntitlement(prefix + String.format("%s:role=%s", imsConfig.group(), opsco) + postfix);

        try {
            builder.addAttribute("userinfo", mapper.writeValueAsString(userInfo));
        } catch (JsonProcessingException e) {
            fail(e.getMessage());
        }

        // Parse roles from entitlements
        UniAssertSubscriber<Boolean> subscriber = this.roleCustomization.augment(this.builder.build(), null)
            .onItem().transform(id -> id.getRoles())
            .onItem().transform(roles -> {
                // Check that it has the correct role
                return roles.contains(Role.OPERATIONS_COORDINATOR) &&
                       roles.contains(Role.PROCESS_MEMBER) &&
                       roles.contains(Role.IMS_USER);
            })
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

        subscriber
            .awaitItem()
            .assertItem(true);
    }
}
