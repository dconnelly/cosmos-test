package yammer.cosmos;

import com.microsoft.azure.cosmos.*;
import com.microsoft.azure.cosmosdb.ConnectionMode;
import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import org.junit.*;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class QueryTest {
    private static final String ENDPOINT = System.getProperty("cosmosEndpoint");
    private static final String KEY = System.getProperty("cosmosKey");
    private static final boolean CLEAN = Boolean.getBoolean("clean");

    private static final String DATABASE_ID = "cosmos-test";
    private static final String CONTAINER_ID = "inboxes";
    private static final String PARTITION_KEY_PATH = "/inboxId";
    private static final String INBOX_ID = "joe";

    private static final Logger logger = LoggerFactory.getLogger(QueryTest.class);

    private static CosmosClient client;
    private static CosmosDatabase database;
    private static CosmosContainer inboxes;

    @BeforeClass
    public static void init() {
        ConnectionPolicy policy = new ConnectionPolicy();
        policy.setConnectionMode(ConnectionMode.Direct);
        client = CosmosClient.create(new CosmosConfiguration.Builder()
                .withServiceEndpoint(ENDPOINT)
                .withKeyOrResourceToken(KEY)
                .withConsistencyLevel(ConsistencyLevel.Session)
                .withConnectionPolicy(policy)
                .build());
        if (CLEAN) {
            client.getDatabase(DATABASE_ID)
                    .delete()
                    .then()
                    .onErrorResume(QueryTest::isNotFound, e -> Mono.empty())
                    .block();
        }
        database = client.getDatabase(DATABASE_ID)
                .read()
                .onErrorResume(QueryTest::isNotFound, e -> client.createDatabase(DATABASE_ID)
                        .doOnNext(r -> logger.info("Created database: {}", r.getDatabase().getId())))
                .map(CosmosDatabaseResponse::getDatabase)
                .blockOptional().orElseThrow(AssertionError::new);
        inboxes = database.getContainer(CONTAINER_ID)
                .read()
                .onErrorResume(QueryTest::isNotFound, e -> database.createContainer(CONTAINER_ID, PARTITION_KEY_PATH)
                        .doOnNext(r -> logger.info("Created container: {}", r.getContainer().getId())))
                .map(CosmosContainerResponse::getContainer)
                .blockOptional().orElseThrow(AssertionError::new);
        createEntries(INBOX_ID, 100);
    }

    @AfterClass
    public static void close() {
        Optional.ofNullable(client).ifPresent(CosmosClient::close);
    }

    @Rule
    public final TestName name = new TestName();

    @Before
    public void logTestName() {
        logger.info("*** Running test: {} ***", name.getMethodName());
    }

    private static void createEntries(String inboxId, int maxCount) {
        int count = query(inboxId).withLogging(false).count();
        if (count < maxCount) {
            logger.info("Adding {} new inbox entries", maxCount - count);
            Flux.fromStream(IntStream.range(count, maxCount).boxed())
                    .map(sequence -> new InboxItem(
                            UUID.randomUUID().toString(), inboxId, sequence,
                            sequence % 2 == 0 ? ReadStatus.READ : ReadStatus.UNREAD))
                    .flatMapSequential(item -> inboxes.createItem(item, inboxId), 20)
                    .blockLast();
        }
    }

    private static boolean isNotFound(Throwable e) {
        return e instanceof DocumentClientException && ((DocumentClientException) e).getStatusCode() == 404;
    }

    @Test
    public void testCount() {
        logger.info("Querying inbox item count...");
        int count = query(INBOX_ID).count();
        logger.info("Item count = {}", count);
    }

    @Test
    public void testUnreadCount() {
        logger.info("Querying unread item count");
        int count = query(INBOX_ID).withStatus(ReadStatus.UNREAD).count();
        logger.info("Unread count = {}", count);
    }

    @Test
    public void testList() {
        logger.info("Querying all items...");
        List<InboxItem> items = query(INBOX_ID).list();
        logger.info("Item count = {}", items.size());
    }

    @Test
    public void testListUnread() {
        logger.info("Querying unread items...");
        List<InboxItem> items = query(INBOX_ID).withStatus(ReadStatus.UNREAD).list();
        logger.info("Unread item count = {}", items.size());
    }

    private static InboxQuery query(String inboxId) {
        return new InboxQuery(inboxes, inboxId).withLogging(true);
    }
}
