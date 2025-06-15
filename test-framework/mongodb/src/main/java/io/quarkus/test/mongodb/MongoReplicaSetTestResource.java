package io.quarkus.test.mongodb;

import static org.awaitility.Durations.*;

import java.net.UnknownHostException;
import java.util.*;

import org.awaitility.Awaitility;
import org.bson.Document;
import org.jboss.logging.Logger;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoReplicaSetTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String REPLICA_SET = "replicaSet";
    static final String DEFAULT_REPLICA_SET = "test001";
    private static final Logger LOGGER = Logger.getLogger(MongoReplicaSetTestResource.class);
    private Integer port;
    private IFeatureAwareVersion version;

    private String replicaSet;

    private List<TransitionWalker.ReachedState<RunningMongodProcess>> startedServers = Collections.emptyList();

    private static Net net(String hostName, int port) {
        return Net.builder().from(Net.defaults()).bindIp(hostName).port(port).build();
    }

    public static String setReplicaSet(Map<String, String> initArgs) {
        return Optional.ofNullable(initArgs.get(REPLICA_SET)).orElse(DEFAULT_REPLICA_SET);
    }

    private static List<TransitionWalker.ReachedState<RunningMongodProcess>> startReplicaSet(
            IFeatureAwareVersion version, int basePort, String replicaSet) {
        TransitionWalker.ReachedState<RunningMongodProcess> firstStarted = mongodWithPort(basePort, replicaSet)
                .start(version);
        try {
            TransitionWalker.ReachedState<RunningMongodProcess> secondStarted = mongodWithPort(basePort + 1, replicaSet)
                    .start(version);

            try {
                ServerAddress firstAddress = firstStarted.current().getServerAddress();
                ServerAddress secondAddress = secondStarted.current().getServerAddress();
                initializeReplicaSet(Arrays.asList(firstAddress, secondAddress), replicaSet);
                LOGGER.infof("ReplicaSet initialized with servers - firstServer: %s , secondServer: %s", firstAddress,
                        secondAddress);
                return Arrays.asList(secondStarted, firstStarted);
            } catch (Exception ex) {
                LOGGER.error("Shutting down second Mongo Server.");
                secondStarted.close();
                LOGGER.errorv(ex, "Error while initializing replicaSet. Error Message %s", ex.getMessage());
                throw new RuntimeException("Error starting second server and initializing replicaset.", ex);
            }
        } catch (RuntimeException rx) {
            LOGGER.error("Shutting down first Mongo Server.");
            firstStarted.close();
            throw rx;
        }
    }

    private static Mongod mongodWithPort(int port, String replicaSet) {
        return Mongod.instance().withNet(Start.to(Net.class).initializedWith(net("localhost", port)))
                .withProcessOutput(Start.to(ProcessOutput.class).initializedWith(ProcessOutput.silent()))
                .withMongodArguments(Start.to(MongodArguments.class)
                        .initializedWith(MongodArguments.defaults().withArgs(Map.of("--replSet", replicaSet))
                                .withSyncDelay(10).withUseSmallFiles(true).withUseNoJournal(false)));
    }

    private static void initializeReplicaSet(final List<ServerAddress> mongodConfigList, String replicaSet)
            throws UnknownHostException {
        String arbitrerAddress = "mongodb://" + mongodConfigList.get(0).getHost() + ":"
                + mongodConfigList.get(0).getPort();
        MongoClientSettings mo = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(arbitrerAddress)).build();

        try (MongoClient mongo = MongoClients.create(mo)) {
            MongoDatabase mongoAdminDB = mongo.getDatabase("admin");

            Document cr = mongoAdminDB.runCommand(new Document("isMaster", 1));
            LOGGER.infof("isMaster: %s", cr);

            // Build replica set configuration settings
            Document rsConfiguration = buildReplicaSetConfiguration(mongodConfigList, replicaSet);
            LOGGER.infof("replSetSettings: %s", rsConfiguration);

            // Initialize replica set
            cr = mongoAdminDB.runCommand(new Document("replSetInitiate", rsConfiguration));
            LOGGER.infof("replSetInitiate: %s", cr);

            // Check replica set status before to proceed
            Awaitility.await().atMost(ONE_MINUTE).with().pollInterval(ONE_SECOND).until(() -> {
                Document result = mongoAdminDB.runCommand(new Document("replSetGetStatus", 1));
                LOGGER.infof("replSetGetStatus: %s", result);
                boolean replicaSetStatus = isReplicaSetStarted(result);
                LOGGER.infof("replicaSet Readiness Status: %s", replicaSetStatus);
                return replicaSetStatus;
            });
            LOGGER.info("ReplicaSet is now ready with 2 cluster node.");
        }
    }

    private static Document buildReplicaSetConfiguration(List<ServerAddress> configList, String replicaSet)
            throws UnknownHostException {
        Document replicaSetSetting = new Document();
        replicaSetSetting.append("_id", replicaSet);

        List<Document> members = new ArrayList<>();
        int i = 0;
        for (ServerAddress mongoConfig : configList) {
            members.add(new Document().append("_id", i++).append("host",
                    mongoConfig.getHost() + ":" + mongoConfig.getPort()));
        }

        replicaSetSetting.append("members", members);
        LOGGER.infof("ReplicaSet Configuration settings: %s", replicaSetSetting);
        return replicaSetSetting;
    }

    private static boolean isReplicaSetStarted(Document setting) {
        if (!setting.containsKey("members")) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Document> members = setting.get("members", List.class);
        for (Document member : members) {
            LOGGER.infof("replica set member %s", member);
            int state = member.getInteger("state");
            LOGGER.infof("state: %s", state);
            // 1 - PRIMARY, 2 - SECONDARY, 7 - ARBITER
            if (state != 1 && state != 2 && state != 7) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void init(Map<String, String> initArgs) {
        port = MongoTestResource.port(initArgs);
        version = MongoTestResource.version(initArgs);
        replicaSet = setReplicaSet(initArgs);
    }

    @Override
    public Map<String, String> start() {
        MongoTestResource.forceExtendedSocketOptionsClassInit();

        startedServers = startReplicaSet(version, port, replicaSet);

        return Collections.singletonMap("quarkus.mongodb.hosts", String.format("127.0.0.1:%d", port));
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down embedded mongo severs...");
        for (TransitionWalker.ReachedState<RunningMongodProcess> startedServer : startedServers) {
            LOGGER.infof("Shutting down embedded mongo server : %s", startedServer);
            startedServer.close();
        }
        startedServers = Collections.emptyList();
    }

}
