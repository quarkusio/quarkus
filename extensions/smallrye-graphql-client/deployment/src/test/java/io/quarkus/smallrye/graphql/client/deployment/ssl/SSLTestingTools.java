package io.quarkus.smallrye.graphql.client.deployment.ssl;

import java.security.KeyStore;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.smallrye.graphql.client.vertx.ssl.SSLTools;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;

public class SSLTestingTools {
    private Vertx vertx;

    public HttpServer runServer(String keystorePath, String keystorePassword,
            String truststorePath, String truststorePassword)
            throws InterruptedException, ExecutionException, TimeoutException {
        vertx = Vertx.vertx();
        HttpServerOptions options = new HttpServerOptions();
        options.setSsl(true);
        options.setHost("localhost");

        if (keystorePath != null) {
            PfxOptions keystoreOptions = new PfxOptions();
            KeyStore keyStore = SSLTools.createKeyStore(keystorePath, "PKCS12", keystorePassword);
            keystoreOptions.setValue(SSLTools.asBuffer(keyStore, keystorePassword.toCharArray()));
            keystoreOptions.setPassword(keystorePassword);
            options.setKeyCertOptions(keystoreOptions);
        }

        if (truststorePath != null) {
            options.setClientAuth(ClientAuth.REQUIRED);
            PfxOptions truststoreOptions = new PfxOptions();
            KeyStore trustStore = SSLTools.createKeyStore(truststorePath, "PKCS12", truststorePassword);
            truststoreOptions.setValue(SSLTools.asBuffer(trustStore, truststorePassword.toCharArray()));
            truststoreOptions.setPassword(truststorePassword);
            options.setTrustOptions(truststoreOptions);
        }

        HttpServer server = vertx.createHttpServer(options);
        server.requestHandler(request -> {
            request.response().send("{\n" +
                    "  \"data\": {\n" +
                    "    \"result\": \"HelloWorld\"\n" +
                    "  }\n" +
                    "}");
        });

        return server.listen(63805).toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    public void close() {
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }
}
