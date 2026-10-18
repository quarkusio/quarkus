package io.quarkus.test.mongodb;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.types.ProcessConfig;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class MongoTestResource implements QuarkusTestResourceLifecycleManager {
    public static final String PORT = "port";
    public static final String VERSION = "version";
    static final int DEFAULT_PORT = 27017;

    private static final Logger LOGGER = Logger.getLogger(MongoTestResource.class);

    private Integer port;
    private IFeatureAwareVersion version;

    private TransitionWalker.ReachedState<RunningMongodProcess> startedServer;

    public static int port(Map<String, String> initArgs) {
        return Optional.ofNullable(initArgs.get(PORT)).map(Integer::parseInt).orElse(DEFAULT_PORT);
    }

    public static IFeatureAwareVersion version(Map<String, String> initArgs) {
        Optional<String> versionArg = Optional.ofNullable(initArgs.get(VERSION));

        return versionArg.<IFeatureAwareVersion> map(Version.Main::valueOf)
                .orElseGet(() -> versionArg.map(
                        versionStr -> Versions.withFeatures(de.flapdoodle.embed.process.distribution.Version.of(versionStr)))
                        .orElse(Version.Main.V7_0));
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

    @Override
    public void init(Map<String, String> initArgs) {
        port = port(initArgs);
        version = version(initArgs);
    }

    @Override
    public Map<String, String> start() {
        forceExtendedSocketOptionsClassInit();

        LOGGER.infof("Starting Mongo %s on port %s", version, port);

        startedServer = Mongod.instance().withNet(Start.to(Net.class)
                .initializedWith(Net.builder().from(Net.defaults()).port(port).build()))
                .withMongodArguments(Start.to(MongodArguments.class)
                        .initializedWith(MongodArguments.defaults().withUseNoJournal(false)))
                .withProcessConfig(
                        Start.to(ProcessConfig.class).initializedWith(ProcessConfig.defaults().withStopTimeoutInMillis(15_000)))
                .start(version);

        return Collections.singletonMap("quarkus.mongodb.hosts", String.format("127.0.0.1:%d", port));
    }

    @Override
    public void stop() {
        if (startedServer != null) {
            startedServer.close();
            startedServer = null;
        }
    }
}
