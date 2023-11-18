package egi.eu.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import egi.checkin.model.CheckinUser;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Review of an entity
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Review<T> extends GenericEntity<T> {

    public String kind;

    public Long id;

    public Long reviewedEntityId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public T reviewedEntity;

    @Schema(description="Date and time of the review. Always returned as UTC date and time.")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS", timezone = "UTC")
    public LocalDateTime date; // UTC

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<CheckinUser> reviewers;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String reviewNotes;

    public Boolean foundInconsistencies;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String reviewFollowUpActions;

    // Links
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<String> links  = null;


    /**
     * Constructor
     */
    public Review() {
        super(null, "Review", false);
    }

}
