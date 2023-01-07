package io.quarkus.test.mongodb;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {
    public static final String PORT = "port";
    public static final String VERSION = "version";
    private static final int DEFAULT_PORT = 27017;

    private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);
    private Integer port;
    private IFeatureAwareVersion version;

    private TransitionWalker.ReachedState<RunningMongodProcess> started;

    @Override
    public void init(Map<String, String> initArgs) {
        port = Optional.ofNullable(initArgs.get(PORT)).map(Integer::parseInt).orElse(DEFAULT_PORT);
        version = Optional.ofNullable(initArgs.get(VERSION)).map(versionStr -> {
            try {
                return Version.valueOf(versionStr);
            } catch (IllegalArgumentException e) {
                try {
                    return Version.Main.valueOf(versionStr);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            String.format("Unable to convert %s to a known Mongo version", versionStr));
                }
            }
        }).orElse(Version.Main.V4_0);
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
