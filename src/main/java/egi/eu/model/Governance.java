package egi.eu.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import egi.eu.entity.GovernanceEntity;


/***
 * Information about the IMS governance
 */
public class Governance extends VersionInfo {

    @Schema(enumeration={ "Governance" })
    public String kind = "Governance";

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Long id;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String title;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String description; // Markdown

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Set<Annex> annexes;

    // Change history
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public HistoryOfGovernance history = null;


    /***
     * History of the governance details
     */
    public static class HistoryOfGovernance extends History<Governance> {
        public HistoryOfGovernance() { super(); }
        public HistoryOfGovernance(List<Governance> olderVersions) { super(olderVersions); }
    }

    /***
     * Constructor
     */
    public Governance() {}

    /***
     * Copy constructor
     * @param governance The entity to copy
     */
    public Governance(GovernanceEntity governance) {

        if(null == governance)
            return;

        this.id = governance.id;
        this.title = governance.title;
        this.description = governance.description;

        if(null != governance.annexes)
            this.annexes = governance.annexes.stream().map(Governance.Annex::new).collect(Collectors.toSet());

        this.version = governance.version;
        this.changedOn = governance.changedOn;
        this.changeDescription = governance.changeDescription;
        if(null != governance.changeBy)
            this.changeBy = new User(governance.changeBy);
    }

    /***
     * Construct from history.
     * @param governanceVersions The list of versions, should start with the latest version.
     */
    public Governance(List<GovernanceEntity> governanceVersions) {
        // Head of the list as the current version
        this(governanceVersions.get(0));

        // The rest of the list as the history of this entity
        var olderVersions = governanceVersions.stream().skip(1).map(Governance::new).toList();
        if(!olderVersions.isEmpty())
            this.history = new HistoryOfGovernance(olderVersions);
    }


    /***
     * Some annex to the governance
     */
    public static class Annex {

        @Schema(enumeration={ "Annex" })
        public String kind = "Annex";

        public Long id;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String body; // Markdown

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String composition; // Markdown

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String meeting; // Markdown

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String decisionVoting; // Markdown

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Set<Interface> interfaces;


        /***
         * Constructor
         */
        public Annex() {}

        /***
         * Copy constructor
         */
        public Annex(GovernanceEntity.Annex annex) {
            this.id = annex.id;
            this.body = annex.body;
            this.composition = annex.composition;
            this.meeting = annex.meeting;
            this.decisionVoting = annex.decisionVoting;

            if(null != annex.interfaces)
                this.interfaces = annex.interfaces.stream().map(Interface::new).collect(Collectors.toSet());
        }


        /***
         * A relationship of an annex to the governance
         */
        public static class Interface {

            @Schema(enumeration = { "Interface" })
            public String kind = "Interface";

            public Long id;

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            public String interfacesWith;

            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            public String comment;


            /***
             * Constructor
             */
            public Interface() {}

            /***
             * Copy constructor
             */
            public Interface(GovernanceEntity.Annex.Interface itf) {
                this.id = itf.id;
                this.interfacesWith = itf.interfacesWith;
                this.comment = itf.comment;
            }
        }
    }
}
