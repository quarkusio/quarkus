package io.quarkus.test.mongodb;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);
    private Integer port;
    private IFeatureAwareVersion version;

    private TransitionWalker.ReachedState<RunningMongodProcess> started;

    @Override
    public void init(Map<String, String> initArgs) {
        port = InitArgs.port(initArgs);
        version = InitArgs.version(initArgs);
    }

    @Override
    public Map<String, String> start() {
        Issue14424.fix();
        
        LOGGER.infof("Starting Mongo %s on port %s", version, port);

        started = Mongod.instance()
          .withNet(Start.to(Net.class).initializedWith(Net.builder()
              .from(Net.defaults())
            .port(port)
            .build()))
          .withMongodArguments(Start.to(MongodArguments.class)
            .initializedWith(MongodArguments.defaults().withUseNoJournal(false)))
          .start(version);

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (started!=null) {
            started.close();
        }
    }
}
