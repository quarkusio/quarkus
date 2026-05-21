package io.quarkus.flyway.mongodb.test;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.types.ProcessConfig;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;

class FlapdoodleMongodbExtension implements BeforeAllCallback {

    static final int MONGO_PORT = 27018;
    static final String MONGO_CONNECTION_STRING = "mongodb://127.0.0.1:" + MONGO_PORT;

    private static final Logger LOGGER = Logger.getLogger(FlapdoodleMongodbExtension.class);
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(FlapdoodleMongodbExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
        if (store.get(MongodHandle.class) == null) {
            store.put(MongodHandle.class, new MongodHandle());
        }
    }

    private static final class MongodHandle implements AutoCloseable {

        private final TransitionWalker.ReachedState<RunningMongodProcess> mongod;

        MongodHandle() {
            LOGGER.infof("Starting embedded Mongo %s on port %d", Version.Main.V7_0, MONGO_PORT);
            mongod = Mongod.instance()
                    .withNet(Start.to(Net.class)
                            .initializedWith(Net.builder().from(Net.defaults()).port(MONGO_PORT).build()))
                    .withMongodArguments(Start.to(MongodArguments.class)
                            .initializedWith(MongodArguments.defaults().withUseNoJournal(false)))
                    .withProcessConfig(Start.to(ProcessConfig.class)
                            .initializedWith(ProcessConfig.defaults().withStopTimeoutInMillis(15_000)))
                    .start(Version.Main.V7_0);
        }

        @Override
        public void close() {
            LOGGER.info("Stopping embedded Mongo");
            mongod.close();
        }
    }
}
