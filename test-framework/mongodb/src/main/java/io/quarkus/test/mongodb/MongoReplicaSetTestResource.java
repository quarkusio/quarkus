package io.quarkus.test.mongodb;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bson.Document;
import org.jboss.logging.Logger;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoReplicaSetTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = Logger.getLogger(MongoReplicaSetTestResource.class);
    private static List<MongodExecutable> MONGOS = new ArrayList<>();

    @Override
    public Map<String, String> start() {
        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
        }
        try {
            List<IMongodConfig> configs = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int port = 27017 + i;
                configs.add(buildMongodConfiguration("localhost", port, true));
            }
            configs.forEach(config -> {
                MongodExecutable exec = MongodStarter.getDefaultInstance().prepare(config);
                MONGOS.add(exec);
                try {
                    exec.start();
                } catch (IOException e) {
                    LOGGER.error("Unable to start the mongo instance", e);
                }
            });
            initializeReplicaSet(configs);
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        MONGOS.forEach(mongod -> {
            try {
                mongod.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop MongoDB", e);
            }
        });
    }

    private static void initializeReplicaSet(final List<IMongodConfig> mongodConfigList) throws UnknownHostException {
        final String arbitrerAddress = "mongodb://" + mongodConfigList.get(0).net().getServerAddress().getHostName() + ":"
                + mongodConfigList.get(0).net().getPort();
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

    private static Document buildReplicaSetConfiguration(final List<IMongodConfig> configList) throws UnknownHostException {
        final Document replicaSetSetting = new Document();
        replicaSetSetting.append("_id", "test001");

        final List<Document> members = new ArrayList<>();
        int i = 0;
        for (final IMongodConfig mongoConfig : configList) {
            members.add(new Document().append("_id", i++).append("host",
                    mongoConfig.net().getServerAddress().getHostName() + ":" + mongoConfig.net().getPort()));
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

    private static IMongodConfig buildMongodConfiguration(String url, int port, final boolean configureReplicaSet)
            throws IOException {
        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
        }
        final MongodConfigBuilder builder = new MongodConfigBuilder()
                .version(Version.Main.V4_0)
                .net(new Net(url, port, Network.localhostIsIPv6()));
        if (configureReplicaSet) {
            builder.withLaunchArgument("--replSet", "test001");
            builder.cmdOptions(new MongoCmdOptionsBuilder()
                    .syncDelay(10)
                    .useSmallFiles(true)
                    .useNoJournal(false)
                    .build());
        }
        return builder.build();
    }
}
