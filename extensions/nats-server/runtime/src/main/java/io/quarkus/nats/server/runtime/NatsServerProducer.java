package io.quarkus.nats.server.runtime;

import berlin.yuna.natsserver.config.NatsConfig;
import berlin.yuna.natsserver.config.NatsOptions;
import berlin.yuna.natsserver.config.NatsOptionsBuilder;
import berlin.yuna.natsserver.logic.Nats;
import berlin.yuna.natsserver.logic.NatsUtils;
import berlin.yuna.natsserver.model.exception.NatsStartException;
import jakarta.enterprise.inject.Produces;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static berlin.yuna.natsserver.config.NatsConfig.NATS_BINARY_PATH;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_DOWNLOAD_URL;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_LOG_NAME;
import static berlin.yuna.natsserver.config.NatsConfig.NATS_PROPERTY_FILE;
import static berlin.yuna.natsserver.config.NatsConfig.NET;
import static berlin.yuna.natsserver.config.NatsConfig.PORT;
import static berlin.yuna.natsserver.config.NatsConfig.SERVER_NAME;
import static berlin.yuna.natsserver.config.NatsOptions.natsBuilder;
import static berlin.yuna.natsserver.logic.NatsUtils.isNotEmpty;
import static java.util.Optional.ofNullable;

public class NatsServerProducer {

    private NatsServerConfig natsServerConfig;

    private static final List<Nats> NATS_SERVER_LIST = new CopyOnWriteArrayList<>();

    @Produces
    public Nats produceNatsServer() {
        return ofNullable(natsServerConfig).map(config -> {
            final NatsOptionsBuilder options = natsBuilder();
            if (config.port != (Integer) PORT.defaultValue()) {
                options.config(PORT, String.valueOf(config.port));
            }
            options.config(config.config)
                    .timeoutMs(config.timeoutMs)
                    .version(isNotEmpty(config.version) ? config.version : options.version());
            configure(options, NATS_PROPERTY_FILE, config.configFile);
            configure(options, NATS_BINARY_PATH, config.binaryFile);
            configure(options, NATS_DOWNLOAD_URL, config.downloadUrl);
            configure(options, SERVER_NAME, config.name);

            try {
                final Nats nats = start(options.build(), config);
                NATS_SERVER_LIST.add(nats);
                return nats;
            } catch (Exception e) {
                throw new NatsStartException(e);
            }
        }).orElse(null);
    }

    public void setNatsServerConfig(final NatsServerConfig natsServerConfig) {
        this.natsServerConfig = natsServerConfig;
    }

    /**
     * Returns last running {@link Nats}
     *
     * @return {@link Nats} or null if no server is running
     */
    public static Nats getNatsServer() {
        return NATS_SERVER_LIST.isEmpty() ? null : NATS_SERVER_LIST.get(NATS_SERVER_LIST.size() - 1);
    }

    /**
     * Returns first running {@link Nats} with name
     *
     * @return {@link Nats} or null on no match
     */
    public static Nats getNatsServerByName(final String name) {
        return getNatsServerBy(natsServer -> natsServer.getValue(NATS_LOG_NAME).equals(name));
    }

    /**
     * Returns first running {@link Nats} with pid (processId)
     *
     * @return {@link Nats} or null on no match
     */
    public static Nats getNatsServerByPid(final int pid) {
        return getNatsServerBy(natsServer -> natsServer.pid() == pid);
    }

    /**
     * Returns first running {@link Nats} with port (For random port please identify by name)
     *
     * @return {@link Nats} or null on no match
     */
    public static Nats getNatsServerByPort(final int port) {
        return getNatsServerBy(natsServer -> natsServer.port() == port);
    }

    /**
     * Returns first running {@link Nats} with host
     *
     * @return {@link Nats} or null on no match
     */
    public static Nats getNatsServerByHost(final String host) {
        return getNatsServerBy(natsServer -> natsServer.getValue(NET, () -> "N/A").equals(host));
    }

    /**
     * Returns first running {@link Nats} with filter
     *
     * @return {@link Nats} or null on no match
     */
    public static Nats getNatsServerBy(final Predicate<Nats> filter) {
        return NATS_SERVER_LIST.stream().filter(filter).findFirst().orElse(null);
    }

    private void configure(final NatsOptionsBuilder options, final NatsConfig key, final String value) {
        if (isNotEmpty(value)) {
            options.config(key, value);
        }
    }

    private Nats start(final NatsOptions options, final NatsServerConfig config) {
        final String stayAliveName = ofNullable(options.config().get(NATS_LOG_NAME)).filter(NatsUtils::isNotEmpty).orElse(NATS_LOG_NAME.defaultValueStr());
        final Nats prevNatsServer = config.keepAlive ? getNatsServerByName(stayAliveName) : null;
        return Objects.requireNonNullElseGet(prevNatsServer, () -> new Nats(options));
    }
}
