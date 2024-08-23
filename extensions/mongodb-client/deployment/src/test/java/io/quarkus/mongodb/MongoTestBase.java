package io.quarkus.mongodb;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;

public class MongoTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoTestBase.class);
    private static TransitionWalker.ReachedState<RunningMongodProcess> MONGO;

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
    public static void startMongoDatabase() throws IOException {
        forceExtendedSocketOptionsClassInit();

        String uri = getConfiguredConnectionString();
        // This switch allow testing against a running mongo database.
        if (uri == null) {
            Version.Main version = Version.Main.V4_4;
            int port = 27018;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);

            MONGO = Mongod.instance()
                    .withNet(Start.to(Net.class).initializedWith(Net.builder()
                            .from(Net.defaults())
                            .port(port)
                            .build()))
                    .withMongodArguments(Start.to(MongodArguments.class)
                            .initializedWith(MongodArguments.defaults().withUseNoJournal(false)))
                    .start(version);

        } else {
            LOGGER.infof("Using existing Mongo %s", uri);
        }
    }

    @AfterAll
    public static void stopMongoDatabase() {
        if (MONGO != null) {
            try {
                MONGO.close();
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
