package io.quarkus.test.mongodb;

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
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MongoReplicaSetTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(MongoReplicaSetTestResource.class);
    private Integer port;
    private IFeatureAwareVersion version;

    private List<TransitionWalker.ReachedState<RunningMongodProcess>> startedServers=Arrays.asList();

    @Override
    public void init(Map<String, String> initArgs) {
        port = InitArgs.port(initArgs);
        version = InitArgs.version(initArgs);
    }

    @Override
    public Map<String, String> start() {
        Issue14424.fix();

        this.startedServers = startReplicaSet(version, port);

        return Collections.emptyMap();
    }

    private static Net net(String hostName, int port) {
        return Net.builder()
          .from(Net.defaults())
          .bindIp(hostName)
          .port(port)
          .build();
    }

    private static List<TransitionWalker.ReachedState<RunningMongodProcess>> startReplicaSet(IFeatureAwareVersion version, int basePort) {
        TransitionWalker.ReachedState<RunningMongodProcess> firstStarted = mongodWithPort(basePort)
          .start(version);
        try {
            TransitionWalker.ReachedState<RunningMongodProcess> secondStarted = mongodWithPort(basePort + 1)
              .start(version);

            try {
                ServerAddress firstAddress = firstStarted.current().getServerAddress();
                ServerAddress secondAddress = secondStarted.current().getServerAddress();
                initializeReplicaSet(Arrays.asList(firstAddress, secondAddress));
                return Arrays.asList(secondStarted, firstStarted);
            }
            catch (IOException iox) {
                throw new RuntimeException("could not get server address", iox);
            }
        } catch (RuntimeException rx) {
            firstStarted.close();
            throw rx;
        }
    }

    private static Mongod mongodWithPort(int port) {
        return Mongod.instance()
          .withNet(Start.to(Net.class)
            .initializedWith(net("localhost", port)))
          .withProcessOutput(Start.to(ProcessOutput.class)
            .initializedWith(ProcessOutput.silent()))
          .withMongodArguments(Start.to(MongodArguments.class)
            .initializedWith(MongodArguments.defaults()
              .withArgs(Map.of("--replSet", "test001"))
              .withSyncDelay(10)
              .withUseSmallFiles(true)
              .withUseNoJournal(false)
            ));
    }

    @Override
    public void stop() {
        for (TransitionWalker.ReachedState<RunningMongodProcess> startedServer : startedServers) {
            startedServer.close();
        }
    }

    private static void initializeReplicaSet(final List<ServerAddress> mongodConfigList) throws UnknownHostException {
        final String arbitrerAddress = "mongodb://" + mongodConfigList.get(0).getHost() + ":"
                + mongodConfigList.get(0).getPort();
        final MongoClientSettings mo = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(arbitrerAddress)).build();

        try (MongoClient mongo = MongoClients.create(mo)) {
            final MongoDatabase mongoAdminDB = mongo.getDatabase("admin");

            Document cr = mongoAdminDB.runCommand(new Document("isMaster", 1));
            LOGGER.infof("isMaster: %s", cr);

            // Build replica set configuration settings
            final Document rsConfiguration = buildReplicaSetConfiguration(mongodConfigList);
            LOGGER.infof("replSetSettings: %s", rsConfiguration);

            // Initialize replica set
            cr = mongoAdminDB.runCommand(new Document("replSetInitiate", rsConfiguration));
            LOGGER.infof("replSetInitiate: %s", cr);

            // Check replica set status before to proceed
            Awaitility.await()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(1, TimeUnit.MINUTES)
                    .until(() -> {
                        Document result = mongoAdminDB.runCommand(new Document("replSetGetStatus", 1));
                        LOGGER.infof("replSetGetStatus: %s", result);
                        return !isReplicaSetStarted(result);
                    });
        }
    }

    private static Document buildReplicaSetConfiguration(final List<ServerAddress> configList) throws UnknownHostException {
        final Document replicaSetSetting = new Document();
        replicaSetSetting.append("_id", "test001");

        final List<Document> members = new ArrayList<>();
        int i = 0;
        for (final ServerAddress mongoConfig : configList) {
            members.add(new Document().append("_id", i++).append("host",
                    mongoConfig.getHost() + ":" + mongoConfig.getPort()));
        }

        replicaSetSetting.append("members", members);
        return replicaSetSetting;
    }

    private static boolean isReplicaSetStarted(final Document setting) {
        if (!setting.containsKey("members")) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final List<Document> members = setting.get("members", List.class);
        for (final Document member : members) {
            LOGGER.infof("replica set member %s", member);
            final int state = member.getInteger("state");
            LOGGER.infof("state: %s", state);
            // 1 - PRIMARY, 2 - SECONDARY, 7 - ARBITER
            if (state != 1 && state != 2 && state != 7) {
                return false;
            }
        }
        return true;
    }


}
