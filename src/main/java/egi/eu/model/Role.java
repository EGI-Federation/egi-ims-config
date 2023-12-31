package egi.eu.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

import egi.eu.entity.RoleEntity;


/***
 * The roles that will govern access to the features in section Management System
 */
public class Role extends VersionInfo {

    // Assignable
    public final static String IMS_OWNER = "ims-owner";
    public final static String IMS_MANAGER = "ims-manager";
    public final static String IMS_DEVELOPER = "ims-developer";
    public final static String STRATEGY_COORDINATOR = "strategy-coordinator";
    public final static String OPERATIONS_COORDINATOR = "operations-coordinator";

    // Abstract
    public final static String PROCESS_OWNER = "process-owner";
    public final static String PROCESS_MANAGER = "process-manager";
    public final static String REPORT_OWNER = "report-owner";
    public final static String SERVICE_OWNER = "service-owner";

    // Pseudo-roles that can be used in API endpoint annotations to define access,
    // but are not considered/returned by the API endpoints nor stored in Check-in
    public final static String IMS_USER = "ims";                  // Marks membership in the VO
    public final static String PROCESS_MEMBER = "process-staff";  // Marks membership in the group

    public enum RoleStatus {
        DRAFT(0),
        IMPLEMENTED(1),
        DEPRECATED(2);

        private final int value;
        private RoleStatus(int value) { this.value = value; }
        public int getValue() { return value; }
        public static RoleStatus of(int value) {
            return switch(value) {
                case 1 -> IMPLEMENTED;
                case 2 -> DEPRECATED;
                default -> DRAFT;
            };
        }
    }

    public enum RoleCategory {
        IMS(0),
        PROCESS(1),
        SERVICE(2);

        private final int value;
        private RoleCategory(int value) { this.value = value; }
        public int getValue() { return value; }
        public static RoleCategory of(int value) {
            return switch(value) {
                case 0 -> IMS;
                case 2 -> SERVICE;
                default -> PROCESS;
            };
        }
    }

    @Schema(enumeration={ "Role" })
    public String kind = "Role";

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Long id;

    public String role; // One of the constants from above

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String name; // Human-readable version of the role field

    public boolean assignable;
    public boolean handover = false;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String recommendation; // Markdown

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String tasks; // Markdown

    @Schema(description="Users that hold this role")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<User> users;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public RoleCategory category = null;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public RoleStatus status = null;

    // Change history
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public HistoryOfRole history = null;


    /***
     * History of a role
     */
    public static class HistoryOfRole extends History<Role> {
        public HistoryOfRole() { super(); }
        public HistoryOfRole(List<Role> olderVersions) { super(olderVersions); }
    }


    /***
     * Constructor
     */
    public Role() {
        this.status = RoleStatus.DRAFT;
    }

    /***
     * Construct with a role
     */
    public Role(String role) {
        this.role = role;
        this.status = RoleStatus.DRAFT;
    }

    /***
     * Copy constructor
     * @param role The entity to copy
     */
    public Role(RoleEntity role) {

        this.id = role.id;
        this.role = role.role;
        this.name = role.name;
        this.recommendation = role.recommendation;
        this.tasks = role.tasks;
        this.assignable = role.assignable;
        this.handover = role.handover;
        this.category = RoleCategory.of(role.category);
        this.status = RoleStatus.of(role.status);

        this.version = role.version;
        this.changedOn = role.changedOn;
        this.changeDescription = role.changeDescription;
        if(null != role.changeBy)
            this.changeBy = new User(role.changeBy);
    }

    /***
     * Construct from history.
     * @param roleVersions The list of versions, should start with the latest version.
     */
    public Role(List<RoleEntity> roleVersions) {
        // Head of the list as the current version
        this(roleVersions.get(0));

        // The rest of the list as the history of this entity
        var olderVersions = roleVersions.stream().skip(1).map(Role::new).toList();
        if(!olderVersions.isEmpty())
            this.history = new HistoryOfRole(olderVersions);
    }
}
