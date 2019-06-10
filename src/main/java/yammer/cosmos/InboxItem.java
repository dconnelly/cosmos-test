package yammer.cosmos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InboxItem {
    @JsonProperty
    public final String id;
    @JsonProperty
    public final String inboxId;
    @JsonProperty
    public final long sortKey;
    @JsonProperty
    public final ReadStatus readStatus;

    public InboxItem(@JsonProperty("id") String id,
                     @JsonProperty("inboxId") String inboxId,
                     @JsonProperty("sortKey") long sortKey,
                     @JsonProperty("readStatus") ReadStatus readStatus) {
        this.id = id;
        this.inboxId = inboxId;
        this.sortKey = sortKey;
        this.readStatus = readStatus;
    }
}
