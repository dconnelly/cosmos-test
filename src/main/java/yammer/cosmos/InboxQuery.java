package yammer.cosmos;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.microsoft.azure.cosmos.CosmosContainer;
import com.microsoft.azure.cosmos.CosmosItem;
import com.microsoft.azure.cosmosdb.FeedOptions;
import com.microsoft.azure.cosmosdb.FeedResponse;
import com.microsoft.azure.cosmosdb.PartitionKey;
import com.microsoft.azure.cosmosdb.QueryMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InboxQuery {
    private final CosmosContainer inboxes;
    private final String inboxId;
    private ReadStatus status;
    private boolean logging;

    private static final Logger logger = LoggerFactory.getLogger(InboxQuery.class);

    public InboxQuery(CosmosContainer inboxes, String inboxId) {
        this.inboxes = inboxes;
        this.inboxId = inboxId;
    }

    public InboxQuery withStatus(ReadStatus status) {
        this.status = status;
        return this;
    }

    public InboxQuery withLogging(boolean logging) {
        this.logging = logging;
        return this;
    }

    public int count() {
        return query("VALUE COUNT(1)").get(0).getInt("_aggregate");
    }

    public List<InboxItem> list() {
        return query("*").stream()
                .map(item -> item.toObject(InboxItem.class))
                .collect(Collectors.toList());
    }

    private List<CosmosItem> query(String select) {
        FeedOptions options = new FeedOptions();
        options.setPartitionKey(new PartitionKey(inboxId));
        options.setPopulateQueryMetrics(logging);
        Formatter query = new Formatter()
                .format("SELECT %s FROM c WHERE c.inboxId = '%s'", select, inboxId);
        Optional.ofNullable(status)
                .ifPresent(s -> query.format(" AND c.readStatus = '%s'", status));
        if (logging) {
            logger.info("Query: {}", query);
        }
        Result result = inboxes.queryItems(query.toString(), options)
                .map(Result::new)
                .reduce(Result::add)
                .blockOptional()
                .orElseThrow(AssertionError::new);
        if (logging) {
            logger.info("Query metrics:\n{}", result.metrics);
        }
        return result.items;
    }

    private static class Result {
        final List<CosmosItem> items;
        final QueryMetrics metrics;

        Result(FeedResponse<CosmosItem> response) {
            items = response.getResults();
            metrics = response.getQueryMetrics().values().stream().reduce(QueryMetrics::add).orElse(null);
        }

        Result(List<CosmosItem> items, QueryMetrics metrics) {
            this.items = items;
            this.metrics = metrics;
        }

        Result add(Result other) {
            return new Result(
                    ImmutableList.copyOf(Iterables.concat(items, other.items)),
                    metrics != null ? metrics.add(other.metrics) : null);
        }
    }
}
