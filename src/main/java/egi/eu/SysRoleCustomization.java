package egi.eu;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import egi.eu.model.Role;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.oidc.runtime.AbstractJsonObjectResponse;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import egi.checkin.model.CheckinUser;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;


/***
 * Class to customize role identification from the user information
 * See also https://quarkus.io/guides/security-customization#security-identity-customization
 */
@ApplicationScoped
public class SysRoleCustomization implements SecurityIdentityAugmentor {

    private static final Logger log = Logger.getLogger(SysRoleCustomization.class);

    @Inject
    protected IntegratedManagementSystemConfig config;

    public void setConfig(IntegratedManagementSystemConfig config) {
        this.config = config;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        // NOTE: In case role parsing is a blocking operation, replace with the line below
        // return context.runBlocking(this.build(identity));
        return Uni.createFrom().item(this.build(identity));
    }

    private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
        if(identity.isAnonymous()) {
            return () -> identity;
        } else {
            // Create a new builder and copy principal, attributes, credentials and roles from the original identity
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

            log.debug("Building security identity");

            // Extract the OIDC user information, loaded due to the setting quarkus.roles.source=userinfo
            var ui = identity.getAttribute("userinfo");
            var isAJO = ui instanceof AbstractJsonObjectResponse;
            if(null != ui && (isAJO || ui instanceof String)) {
                // Construct Check-in UserInfo from the user info fetched by OIDC
                CheckinUser userInfo = null;
                String json = null;
                try {
                    var mapper = new ObjectMapper();
                    json = isAJO ? ((AbstractJsonObjectResponse)ui).getJsonObject().toString() : ui.toString();
                    userInfo = mapper.readValue(json, CheckinUser.class);

                    if(null != userInfo.userId)
                        builder.addAttribute(CheckinUser.ATTR_USERID, userInfo.userId);

                    if(null != userInfo.userName)
                        builder.addAttribute(CheckinUser.ATTR_USERNAME, userInfo.userName);

                    if(null != userInfo.firstName)
                        builder.addAttribute(CheckinUser.ATTR_FIRSTNAME, userInfo.firstName);

                    if(null != userInfo.lastName)
                        builder.addAttribute(CheckinUser.ATTR_LASTNAME, userInfo.lastName);

                    if(null != userInfo.email)
                        builder.addAttribute(CheckinUser.ATTR_EMAIL, userInfo.email);

                    builder.addAttribute(CheckinUser.ATTR_EMAILCHECKED, userInfo.emailIsVerified);

                    if(null != userInfo.assurances) {
                        Pattern assuranceRex = Pattern.compile("^https?\\://(aai[^\\.]*.egi.eu)/LoA#([^\\:#/]+)");
                        for(var a : userInfo.assurances) {
                            var matcher = assuranceRex.matcher(a);
                            if(matcher.matches()) {
                                // Got an EGI Check-in backed assurance level
                                var assurance = matcher.group(2);
                                builder.addAttribute(CheckinUser.ATTR_ASSURANCE, assurance.toLowerCase());
                                break;
                            }
                        }
                    }
                }
                catch (JsonProcessingException e) {
                    // Error deserializing JSON info UserInfo instance
                    MDC.put("OIDC.userinfo", null != json ? json : "null");
                    log.warn("Cannot deserialize OIDC userinfo");
                }

                if(null != userInfo) {
                    // Got the Check-in user information, map roles
                    var roleNames = config.roles();

                    final String voPrefix = "urn:mace:egi.eu:group:" + config.vo().toLowerCase() + ":";
                    final String suffix = "#aai.egi.eu";

                    // Only continue checking the roles for members of the configured VO
                    if(userInfo.entitlements.contains(voPrefix + "role=member" + suffix)) {
                        // This user is member of the VO, access to ISM tools is allowed
                        builder.addRole(Role.IMS_USER);

                        final String rolePrefix = voPrefix + config.group() + ":role=";

                        boolean processMember = false;
                        if(userInfo.entitlements.contains(rolePrefix + "member" + suffix)) {
                            // This user is member of the IMS group, which is a prerequisite to holding IMS roles
                            processMember = true;
                            builder.addRole(Role.PROCESS_MEMBER);
                        }

                        final String rexPrefix = "^urn\\:mace\\:egi.eu\\:group\\:" +
                                config.vo().toLowerCase() + "\\:" +
                                config.group() + "\\:role=";

                        final String imso = rolePrefix + roleNames.get("ims-owner").toLowerCase() + suffix;
                        final String imsm = rolePrefix + roleNames.get("ims-manager").toLowerCase() + suffix;
                        final String imsd = rolePrefix + roleNames.get("ims-developer").toLowerCase() + suffix;
                        final String strco = rolePrefix + roleNames.get("strategy-coordinator").toLowerCase() + suffix;
                        final String opsco = rolePrefix + roleNames.get("operations-coordinator").toLowerCase() + suffix;

                        for (var e : userInfo.entitlements) {

                            if (processMember && e.equals(imso))
                                builder.addRole(Role.IMS_OWNER);
                            else if (processMember && e.equals(imsm))
                                builder.addRole(Role.IMS_MANAGER);
                            else if (processMember && e.equals(imsd))
                                builder.addRole(Role.IMS_DEVELOPER);
                            else if (processMember && e.equals(strco))
                                builder.addRole(Role.STRATEGY_COORDINATOR);
                            else if (processMember && e.equals(opsco))
                                builder.addRole(Role.OPERATIONS_COORDINATOR);
                        }
                    } // IMS_USER
                }
            }

            return builder::build;
        }
    }
}
