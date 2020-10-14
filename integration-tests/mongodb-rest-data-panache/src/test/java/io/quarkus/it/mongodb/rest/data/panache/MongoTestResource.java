package io.quarkus.it.mongodb.rest.data.panache;

import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);
    private static MongodExecutable MONGO;

    @Override
    public Map<String, String> start() {
        try {
            Version.Main version = Version.Main.V4_0;
            int port = 27018;
            LOGGER.infof("Starting Mongo %s on port %s", version, port);
            IMongodConfig config = new MongodConfigBuilder()
                    .version(version)
                    .net(new Net(port, Network.localhostIsIPv6()))
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
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MongodExecutable getMongodExecutable(IMongodConfig config) {
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

    private MongodExecutable doGetExecutable(IMongodConfig config) {
        return MongodStarter.getDefaultInstance().prepare(config);
    }

    private void initData() {

    }

    @Override
    public void stop() {
        if (MONGO != null) {
            MONGO.stop();
        }
    }
}
