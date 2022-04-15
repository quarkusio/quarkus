package io.quarkus.test.mongodb;

import static de.flapdoodle.embed.process.config.process.ProcessOutput.builder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongoCmdOptions;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {
    public static final String PORT = "port";
    private static final int DEFAULT_PORT = 27017;
    private static MongodExecutable MONGO;

    private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);
    private Integer port;

    @Override
    public void init(Map<String, String> initArgs) {
        port = Optional.ofNullable(initArgs.get(PORT)).map(Integer::parseInt).orElse(DEFAULT_PORT);
    }

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
            Version.Main version = Version.Main.V4_0;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);
            MongodConfig config = MongodConfig.builder()
                    .version(version)
                    .net(new Net(port, Network.localhostIsIPv6()))
                    .cmdOptions(MongoCmdOptions.builder()
                            .useNoJournal(false)
                            .build())
                    .build();
            MONGO = getMongodExecutable(config);
            try {
                MONGO.start();
            } catch (Exception e) {
                //every so often mongo fails to start on CI runs
                //see if this helps
                Thread.sleep(1000);
                MONGO.start();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    private MongodExecutable getMongodExecutable(MongodConfig config) {
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

    private MongodExecutable doGetExecutable(MongodConfig config) {
        RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
                .processOutput(builder()
                        .output(Processors.silent())
                        .error(Processors.silent())
                        .commands(Processors.silent())
                        .build())
                .build();
        return MongodStarter.getInstance(runtimeConfig).prepare(config);
    }

    @Override
    public void stop() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }
}
