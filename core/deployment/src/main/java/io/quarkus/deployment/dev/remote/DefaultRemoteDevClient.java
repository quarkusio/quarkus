package io.quarkus.deployment.dev.remote;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.dev.spi.RemoteDevState;

/**
 * A noop remote dev client, that just polls every second to update the app
 *
 * This is useful if you are using an external tool such as odo to update your remote pod.
 */
public class DefaultRemoteDevClient implements RemoteDevClient {

    @Override
    public Closeable sendConnectRequest(RemoteDevState currentFileHashes,
            Function<Set<String>, Map<String, byte[]>> initialConnectFunction,
            Supplier<SyncResult> changeRequestFunction) {
        initialConnectFunction.apply(Collections.emptySet());
        return new DefaultSession(changeRequestFunction);
    }

    private static class DefaultSession extends TimerTask implements Closeable {

        private final Supplier<SyncResult> changeRequestFunction;
        private final Timer timer;

        public DefaultSession(Supplier<SyncResult> changeRequestFunction) {
            this.changeRequestFunction = changeRequestFunction;
            timer = new Timer("Remote dev polling timer");
            timer.schedule(this, 1000, 1000);

        }

        @Override
        public void run() {
            //we don't care about the result, we just want to force regeneration
            changeRequestFunction.get();

        }

        @Override
        public void close() {
            timer.cancel();
        }
    }
}
