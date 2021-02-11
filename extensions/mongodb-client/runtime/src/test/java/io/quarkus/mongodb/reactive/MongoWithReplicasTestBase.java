package io.quarkus.mongodb.reactive;

import static io.quarkus.mongodb.reactive.MongoTestBase.getConfiguredConnectionString;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoWithReplicasTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoWithReplicasTestBase.class);
    private static List<MongodExecutable> MONGOS = new ArrayList<>();

    @BeforeAll
    public static void startMongoDatabase() throws IOException {
        String uri = getConfiguredConnectionString();
        // This switch allow testing against a running mongo database.
        if (uri == null) {
            List<IMongodConfig> configs = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int port = 27018 + i;
                configs.add(buildMongodConfiguration("localhost", port, true));
            }
            configs.forEach(config -> {
                MongodExecutable exec = getMongodExecutable(config);
                MONGOS.add(exec);
                try {
                    try {
                        exec.start();
                    } catch (Exception e) {
                        //every so often mongo fails to start on CI runs
                        //see if this helps
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {

                        }
                        exec.start();
                    }
                } catch (IOException e) {
                    LOGGER.error("Unable to start the mongo instance", e);
                }
            });
            initializeReplicaSet(configs);
        } else {
            LOGGER.infof("Using existing Mongo %s", uri);
        }
    }

    private static MongodExecutable getMongodExecutable(IMongodConfig config) {
        try {
            return doGetExecutable(config);
        } catch (Exception e) {
            // sometimes the download process can timeout so just sleep and try again
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            return doGetExecutable(config);
        }
    }

    private static MongodExecutable doGetExecutable(IMongodConfig config) {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaults(Command.MongoD)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        return MongodStarter.getInstance(runtimeConfig).prepare(config);
    }

    @AfterAll
    public static void stopMongoDatabase() {
        MONGOS.forEach(mongod -> {
            try {
                mongod.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop MongoDB", e);
            }
        });
    }

    protected String getConnectionString() {
        if (getConfiguredConnectionString() != null) {
            return getConfiguredConnectionString();
        } else {
            return "mongodb://localhost:27018";
        }
    }

    private static void initializeReplicaSet(final List<IMongodConfig> mongodConfigList) throws UnknownHostException {
        final String arbiterAddress = "mongodb://" + mongodConfigList.get(0).net().getServerAddress().getHostName() + ":"
                + mongodConfigList.get(0).net().getPort();
        final MongoClientSettings mo = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(arbiterAddress)).build();

        try (MongoClient mongo = MongoClients.create(mo)) {
            final MongoDatabase mongoAdminDB = mongo.getDatabase("admin");

            Document cr = mongoAdminDB.runCommand(new Document("isMaster", 1));
            LOGGER.debugf("isMaster: %s", cr);

            // Build replica set configuration settings
            final Document rsConfiguration = buildReplicaSetConfiguration(mongodConfigList);
            LOGGER.debugf("replSetSettings: %s", rsConfiguration);

            // Initialize replica set
            cr = mongoAdminDB.runCommand(new Document("replSetInitiate", rsConfiguration));
            LOGGER.debugf("replSetInitiate: %s", cr);

            // Check replica set status before to proceed
            await()
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
