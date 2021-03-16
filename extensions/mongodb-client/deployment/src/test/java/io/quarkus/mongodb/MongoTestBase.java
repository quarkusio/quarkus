package io.quarkus.mongodb;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;

public class MongoTestBase {

    private static final Logger LOGGER = Logger.getLogger(MongoTestBase.class);
    private static MongodExecutable MONGO;

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
        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
        }
        String uri = getConfiguredConnectionString();
        // This switch allow testing against a running mongo database.
        if (uri == null) {
            Version.Main version = Version.Main.V4_0;
            int port = 27018;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);
            IMongodConfig config = new MongodConfigBuilder()
                    .version(version)
                    .net(new Net("127.0.0.1", port, false))
                    .build();
            MONGO = getMongodExecutable(config);
            try {
                MONGO.start();
            } catch (Exception e) {
                //every so often mongo fails to start on CI runs
                //see if this helps
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {

                }
                MONGO.start();
            }
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
        if (MONGO != null) {
            try {
                MONGO.stop();
            } catch (Exception e) {
                LOGGER.error("Unable to stop MongoDB", e);
            }
        }
    }
}
