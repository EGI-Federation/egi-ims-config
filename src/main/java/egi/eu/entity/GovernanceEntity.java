package egi.eu.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

import egi.eu.Utils;
import egi.eu.model.Governance;


/***
 * Information about the IMS governance
 */
@Entity
@Table(name = "governance")
public class GovernanceEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(length = 256)
    public String title;

    @Column(length = 1048576) // 1M
    public String description;

    @ManyToMany(fetch = FetchType.EAGER,
                cascade = { CascadeType.PERSIST })
    @JoinTable(name = "governance_groups_map",
               joinColumns = { @JoinColumn(name = "governance_id") },
               inverseJoinColumns = { @JoinColumn(name = "group_id") })
    public Set<Group> groups = null;

    // Change tracking
    @Column(nullable = false, unique = true, insertable = false, updatable = false, columnDefinition = "serial")
    public int version;

    @UpdateTimestamp
    public LocalDateTime changedOn;

    @Column(length = 2048)
    public String changeDescription;

    @ManyToOne(fetch = FetchType.EAGER,
               cascade = { CascadeType.PERSIST })
    @JoinTable(name = "governance_editor_map",
            joinColumns = { @JoinColumn(name = "governance_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id") })
    public UserEntity changeBy = null;


    /***
     * Constructor
     */
    public GovernanceEntity() { super(); }

    /***
     * Copy constructor
     * @param governance The governance to copy
     */
    public GovernanceEntity(GovernanceEntity governance) {
        super();

        // Copy simple fields
        this.title = governance.title;
        this.description = governance.description;

        // Copy annexes
        if(null != governance.groups) {
            this.groups = new HashSet<>();
            this.groups.addAll(governance.groups);
        }
    }

    /***
     * Copy constructor
     * @param governance The new version (from the frontend)
     * @param latest The latest version in the database
     * @param users The users that already exist in the database
     */
    public GovernanceEntity(Governance governance, GovernanceEntity latest, Map<String, UserEntity> users) {
        super();

        // Copy simple fields
        this.changeDescription = governance.changeDescription;
        if(null != governance.changeBy) {
            if(null != users && users.containsKey(governance.changeBy.checkinUserId))
                this.changeBy = users.get(governance.changeBy.checkinUserId);
            else
                this.changeBy = new UserEntity(governance.changeBy);
        }

        this.title = governance.title;
        this.description = governance.description;

        // Link to the annexes that stayed the same, create new ones for the ones that changed
        if(null != governance.groups) {
            this.groups = new HashSet<>();
            for(var grpe : governance.groups) {
                // See if there is such an annex in the latest version
                Group grpl = null;
                if(null != latest.groups)
                    for(var ann : latest.groups)
                        if(ann.id.equals(grpe.id)) {
                            grpl = ann;
                            break;
                        }

                if(null == grpl) {
                    // This is a new annex
                    this.groups.add(new Group(grpe, null));
                    continue;
                }

                // See if this annex has changed
                boolean hasChanged = (
                        !Utils.equalStrings(grpe.body, grpl.body) ||
                        !Utils.equalStrings(grpe.composition, grpl.composition) ||
                        !Utils.equalStrings(grpe.meeting, grpl.meeting) ||
                        !Utils.equalStrings(grpe.decisionVoting, grpl.decisionVoting) ||
                       (null == grpe.interfaces) != (null == grpl.interfaces));

                // Link to the interfaces that stayed the same, create new ones for the ones that changed
                Map<Long, Group.Interface> unchangedInterfaces = new HashMap<>();

                if(null != grpe.interfaces && null != grpl.interfaces) {
                    if(grpe.interfaces.size() != grpl.interfaces.size())
                        hasChanged = true;

                    for(var itfe : grpe.interfaces) {
                        boolean itfChanged = true;
                        for(var itfl : grpl.interfaces) {
                            if(itfe.id.equals(itfl.id)) {
                                if(Utils.equalStrings(itfl.interfacesWith, itfe.interfacesWith) &&
                                   Utils.equalStrings(itfl.comment, itfl.comment)) {
                                    // Unchanged, reuse
                                    itfChanged = false;
                                    unchangedInterfaces.put(itfl.id, itfl);
                                    break;
                                }
                            }
                        }

                        if(itfChanged)
                            hasChanged = true;
                    }
                }

                if(hasChanged)
                    this.groups.add(new Group(grpe, unchangedInterfaces));
                else
                    this.groups.add(grpl);
            }
        }
    }

    /***
     * Get the latest version as a list with one element
     * @return List with latest version of the entity
     */
    public static Uni<List<GovernanceEntity>> getLastVersionAsList() {
        return find("ORDER BY version DESC").range(0,0).list();
    }

    /***
     * Get the latest version
     * @return Latest version of the entity
     */
    public static Uni<GovernanceEntity> getLastVersion() {
        return find("ORDER BY version DESC").firstResult();
    }

    /***
     * Get all versions
     * @return All versions of the entity, sorted in reverse chronological order (head of the list is the latest).
     */
    public static Uni<List<GovernanceEntity>> getAllVersions() {
        return find("ORDER BY version DESC").list();
    }

    /***
     * Get all versions, paged
     * @return All versions of the entity, sorted in reverse chronological order (head of the list is the latest).
     */
    public static Uni<List<GovernanceEntity>> getAllVersions(int index, int size) {
        return find("ORDER BY version DESC").page(index, size).list();
    }

    /***
     * Some annex to the governance
     */
    @Entity
    @Table(name = "governance_groups")
    public static class Group extends PanacheEntityBase {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;

        @Column(length = 10240)
        public String body; // Markdown

        @Column(length = 10240)
        public String composition; // Markdown

        @Column(length = 10240)
        public String meeting; // Markdown

        @Column(length = 10240)
        public String decisionVoting; // Markdown

        @ManyToMany(fetch = FetchType.EAGER,
                    cascade = { CascadeType.PERSIST })
        @JoinTable(name = "governance_group_interfaces_map",
                   joinColumns = { @JoinColumn(name = "group_id") },
                   inverseJoinColumns = { @JoinColumn(name = "interface_id") })
        public Set<Interface> interfaces = null;

        /***
         * Constructor
         */
        public Group() { super(); }

        /***
         * Copy constructor
         */
        public Group(Governance.Group group, Map<Long, GovernanceEntity.Group.Interface> unchangedInterfaces) {
            super();

            this.body = group.body;
            this.composition = group.composition;
            this.meeting = group.meeting;
            this.decisionVoting = group.decisionVoting;

            if(null != group.interfaces) {
                this.interfaces = new HashSet<>();
                for(var itf : group.interfaces) {
                    if(null != unchangedInterfaces && unchangedInterfaces.containsKey(itf.id)) {
                        // Interface already exists in the database
                        var itfEntity = unchangedInterfaces.get(itf.id);
                        this.interfaces.add(itfEntity);
                    }
                    else {
                        // New interface
                        var itfEntity = new Group.Interface(itf);
                        this.interfaces.add(itfEntity);
                    }
                }
            }
        }


        /***
         * Interface of an annexes to the governance
         */
        @Entity
        @Table(name = "governance_group_interfaces")
        public static class Interface extends PanacheEntityBase {

            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            public Long id;

            @Schema(enumeration={ "Internal", "External", "Customer",
                    "BA", "BDS", "CAPM", "CHM", "COM", "CONFM", "CSI", "CRM", "CPM", "FA", "HR", "ISM",
                    "ISRM", "PPM", "PM", "PKM", "PPM", "RDM", "RM", "SACM", "SRM", "SLM", "SPM", "SRM" })
            public String interfacesWith;

            @Column(length = 1024)
            public String comment;


            /***
             * Constructor
             */
            public Interface() { super(); }

            /***
             * Copy constructor
             */
            public Interface(Governance.Group.Interface itf) {
                super();

                this.interfacesWith = itf.interfacesWith;
                this.comment = itf.comment;
            }
        }
    }
}
