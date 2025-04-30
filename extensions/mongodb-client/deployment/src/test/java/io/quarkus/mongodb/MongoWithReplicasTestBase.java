package io.quarkus.mongodb;

import static io.quarkus.mongodb.MongoTestBase.getConfiguredConnectionString;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.ONE_SECOND;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.bson.Document;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.types.ProcessConfig;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;

public class MongoWithReplicasTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoWithReplicasTestBase.class);

    private static List<TransitionWalker.ReachedState<RunningMongodProcess>> startedServers = Collections.emptyList();

    @BeforeAll
    public static void startMongoDatabase() {
        String uri = getConfiguredConnectionString();

        // This switch allow testing against a running mongo database.
        if (uri == null) {
            startedServers = startReplicaSet(Version.Main.V7_0, 27018, "test001");
        } else {
            LOGGER.infof("Using existing Mongo %s", uri);
        }
    }

    private static Net net(String hostName, int port) {
        return Net.builder()
                .from(Net.defaults())
                .bindIp(hostName)
                .port(port)
                .build();
    }

    private static List<TransitionWalker.ReachedState<RunningMongodProcess>> startReplicaSet(
            IFeatureAwareVersion version, int basePort, String replicaSet) {
        TransitionWalker.ReachedState<RunningMongodProcess> firstStarted = mongodWithPort(basePort, replicaSet).start(version);
        try {
            TransitionWalker.ReachedState<RunningMongodProcess> secondStarted = mongodWithPort(basePort + 1, replicaSet)
                    .start(version);

            try {
                ServerAddress firstAddress = firstStarted.current().getServerAddress();
                ServerAddress secondAddress = secondStarted.current().getServerAddress();
                initializeReplicaSet(Arrays.asList(firstAddress, secondAddress), replicaSet);
                LOGGER.infof("ReplicaSet initialized with servers - firstServer: %s , secondServer: %s",
                        firstAddress, secondAddress);
                return Arrays.asList(firstStarted, secondStarted);
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
                .withMongodArguments(Start.to(MongodArguments.class).initializedWith(
                        MongodArguments.defaults().withArgs(Map.of("--replSet", replicaSet)).withSyncDelay(10)
                                .withUseSmallFiles(true).withUseNoJournal(false)))
                .withProcessConfig(
                        Start.to(ProcessConfig.class)
                                .initializedWith(ProcessConfig.defaults().withStopTimeoutInMillis(15_000)));
    }

    @AfterAll
    public static void stopMongoDatabase() {
        for (TransitionWalker.ReachedState<RunningMongodProcess> startedServer : startedServers) {
            try {
                startedServer.close();
            } catch (RuntimeException rx) {
                LOGGER.error("startedServer.close", rx);
            }
        }
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
            members.add(new Document().append("_id", i++).append("host", mongoConfig.getHost() + ":" + mongoConfig.getPort()));
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
}
