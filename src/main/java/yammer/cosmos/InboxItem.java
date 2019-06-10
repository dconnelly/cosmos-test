package yammer.cosmos;

import com.fasterxml.jackson.annotation.JsonProperty;

class InboxItem {
    @JsonProperty
    final String id;
    @JsonProperty
    final String inboxId;
    @JsonProperty
    final long sortKey;
    @JsonProperty
    final ReadStatus readStatus;

    InboxItem(@JsonProperty("id") String id,
              @JsonProperty("inboxId") String inboxId,
              @JsonProperty("sortKey") long sortKey,
              @JsonProperty("readStatus") ReadStatus readStatus) {
        this.id = id;
        this.inboxId = inboxId;
        this.sortKey = sortKey;
        this.readStatus = readStatus;
    }
}
