package io.quarkus.reactive.datasource.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.quarkus.credentials.CredentialsProvider;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnectOptions;

public class ConnectOptionsSupplier<CO extends SqlConnectOptions> implements Supplier<Future<CO>> {

    private final CredentialsProvider credentialsProvider;
    private final String credentialsProviderName;
    private final List<CO> connectOptionsList;
    private final UnaryOperator<CO> connectOptionsCopy;
    private final AtomicInteger idx = new AtomicInteger();

    public ConnectOptionsSupplier(CredentialsProvider credentialsProvider, String credentialsProviderName,
            List<CO> connectOptionsList, UnaryOperator<CO> connectOptionsCopy) {
        this.credentialsProvider = credentialsProvider;
        this.credentialsProviderName = credentialsProviderName;
        this.connectOptionsList = connectOptionsList;
        this.connectOptionsCopy = connectOptionsCopy;
    }

    @Override
    public Future<CO> get() {
        int nextIdx = idx.getAndUpdate(previousIdx -> previousIdx == connectOptionsList.size() - 1 ? 0 : previousIdx + 1);
        CO connectOptions = connectOptionsCopy.apply(connectOptionsList.get(nextIdx));
        return Uni.combine()
                .all()
                .unis(credentialsProvider.getCredentialsAsync(credentialsProviderName), Uni.createFrom().item(connectOptions))
                .with((credentials, co) -> {
                    co.setUser(credentials.get(USER_PROPERTY_NAME));
                    co.setPassword(credentials.get(PASSWORD_PROPERTY_NAME));
                    return co;
                })
                .convert()
                .with(UniHelper::toFuture);
    }
}
