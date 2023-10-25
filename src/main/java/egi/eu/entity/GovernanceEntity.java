package egi.eu.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JoinTable(name = "governance_annexes_map",
               joinColumns = { @JoinColumn(name = "governance_id") },
               inverseJoinColumns = { @JoinColumn(name = "annex_id") })
    public Set<Annex> annexes = null;

    // Change tracking
    @Column(nullable = false, unique = true, insertable = false, updatable = false, columnDefinition = "serial")
    public int version;

    @UpdateTimestamp
    public LocalDateTime changedOn;

    @Column(length = 1024)
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
        if(null != governance.annexes) {
            this.annexes = new HashSet<>();
            this.annexes.addAll(governance.annexes);
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
        if(null != governance.annexes) {
            this.annexes = new HashSet<>();
            for(var anne : governance.annexes) {
                // See if there is such an annex in the latest version
                GovernanceEntity.Annex annl = null;
                if(null != latest.annexes)
                    for(var ann : latest.annexes)
                        if(ann.id.equals(anne.id)) {
                            annl = ann;
                            break;
                        }

                if(null == annl) {
                    // This is a new annex
                    this.annexes.add(new Annex(anne, null));
                    continue;
                }

                // See if this annex has changed
                boolean hasChanged = (
                        !Utils.equalStrings(anne.body, annl.body) ||
                        !Utils.equalStrings(anne.composition, annl.composition) ||
                        !Utils.equalStrings(anne.meeting, annl.meeting) ||
                        !Utils.equalStrings(anne.decisionVoting, annl.decisionVoting) ||
                       (null == anne.interfaces) != (null == annl.interfaces));

                // Link to the interfaces that stayed the same, create new ones for the ones that changed
                Map<Long, Annex.Interface> unchangedInterfaces = new HashMap<>();

                if(null != anne.interfaces && null != annl.interfaces) {
                    if(anne.interfaces.size() != annl.interfaces.size())
                        hasChanged = true;

                    for(var itfe : anne.interfaces) {
                        boolean itfChanged = true;
                        for(var itfl : annl.interfaces) {
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
                    this.annexes.add(new Annex(anne, unchangedInterfaces));
                else
                    this.annexes.add(annl);
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
    @Table(name = "governance_annexes")
    public static class Annex extends PanacheEntityBase {

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
        @JoinTable(name = "governance_annex_interfaces_map",
                   joinColumns = { @JoinColumn(name = "annex_id") },
                   inverseJoinColumns = { @JoinColumn(name = "interface_id") })
        public Set<Interface> interfaces = null;

        /***
         * Constructor
         */
        public Annex() { super(); }

        /***
         * Copy constructor
         */
        public Annex(Governance.Annex annex, Map<Long, GovernanceEntity.Annex.Interface> unchangedInterfaces) {
            super();

            this.body = annex.body;
            this.composition = annex.composition;
            this.meeting = annex.meeting;
            this.decisionVoting = annex.decisionVoting;

            if(null != annex.interfaces) {
                this.interfaces = new HashSet<>();
                for(var itf : annex.interfaces) {
                    if(null != unchangedInterfaces && unchangedInterfaces.containsKey(itf.id)) {
                        // Interface already exists in the database
                        var itfEntity = unchangedInterfaces.get(itf.id);
                        this.interfaces.add(itfEntity);
                    }
                    else {
                        // New interface
                        var itfEntity = new Interface(itf);
                        this.interfaces.add(itfEntity);
                    }
                }
            }
        }


        /***
         * Interface of an annexes to the governance
         */
        @Entity
        @Table(name = "governance_annex_interfaces")
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
            public Interface(Governance.Annex.Interface itf) {
                super();

                this.interfacesWith = itf.interfacesWith;
                this.comment = itf.comment;
            }
        }
    }
}
