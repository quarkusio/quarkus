package io.quarkus.mongodb;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.ImmutableMongod;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.types.ProcessConfig;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MongoTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoTestBase.class);
    protected TransitionWalker.ReachedState<RunningMongodProcess> mongo;

    protected static String getConfiguredConnectionString() {
        return getProperty("connection_string");
    }

    protected static String getProperty(String name) {
        String s = System.getProperty(name);
        if (s != null) {
            s = s.trim();
            if (s.length() > 0) {
                return s;
            }
        }

        return null;
    }

    @BeforeAll
    public void startMongoDatabase() {
        forceExtendedSocketOptionsClassInit();

        String uri = getConfiguredConnectionString();
        // This switch allow testing against a running mongo database.
        if (uri == null) {
            Version.Main version = Version.Main.V7_0;
            int port = 27018;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);

            ImmutableMongod config = Mongod.instance()
                    .withNet(Start.to(Net.class).initializedWith(Net.builder()
                            .from(Net.defaults())
                            .port(port)
                            .build()))
                    .withMongodArguments(Start.to(MongodArguments.class)
                            .initializedWith(MongodArguments.defaults()
                                    .withUseNoJournal(
                                            false)))
                    .withProcessConfig(
                            Start.to(ProcessConfig.class)
                                    .initializedWith(ProcessConfig.defaults()
                                            .withStopTimeoutInMillis(15_000)));
            config = addExtraConfig(config);
            mongo = config.start(version);

        } else {
            LOGGER.infof("Using existing Mongo %s", uri);
        }
    }

    protected ImmutableMongod addExtraConfig(ImmutableMongod mongo) {
        return mongo;
    }

    @AfterAll
    public void stopMongoDatabase() {
        if (mongo != null) {
            try {
                mongo.close();
            } catch (Exception e) {
                LOGGER.error("Unable to stop MongoDB", e);
            }
        }
    }

    public static void forceExtendedSocketOptionsClassInit() {
        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
        }
    }
}
