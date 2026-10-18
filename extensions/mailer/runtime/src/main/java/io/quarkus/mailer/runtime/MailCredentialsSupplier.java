package io.quarkus.mailer.runtime;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.function.Supplier;

import io.quarkus.credentials.CredentialsProvider;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

class MailCredentialsSupplier implements Supplier<Future<UsernamePasswordCredentials>> {

    private final CredentialsProvider credentialsProvider;
    private final String credentialsProviderName;

    MailCredentialsSupplier(CredentialsProvider credentialsProvider, String credentialsProviderName) {
        this.credentialsProvider = credentialsProvider;
        this.credentialsProviderName = credentialsProviderName;
    }

    @Override
    public Future<UsernamePasswordCredentials> get() {
        return credentialsProvider.getCredentialsAsync(credentialsProviderName)
                .map(credentials -> new UsernamePasswordCredentials(
                        credentials.get(USER_PROPERTY_NAME),
                        credentials.get(PASSWORD_PROPERTY_NAME)))
                .convert()
                .with(UniHelper::toFuture);
    }
}
