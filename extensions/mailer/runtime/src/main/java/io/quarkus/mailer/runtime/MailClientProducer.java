package io.quarkus.mailer.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.mail.*;

/**
 * Beans producing the Vert.x Mail clients.
 */
@ApplicationScoped
public class MailClientProducer {

    private static final Logger LOGGER = Logger.getLogger(MailClientProducer.class);

    private final io.vertx.mutiny.ext.mail.MailClient mutinyClient;
    private final MailClient client;

    public MailClientProducer(Vertx vertx, MailConfig config, TlsConfig tlsConfig) {
        this.client = mailClient(vertx, config, tlsConfig);
        this.mutinyClient = io.vertx.mutiny.ext.mail.MailClient.newInstance(this.client);
    }

    @Singleton
    @Produces
    public MailClient mailClient() {
        return client;
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.ext.mail.MailClient mutinyClient() {
        return mutinyClient;
    }

    @PreDestroy
    public void stop() {
        client.close();
    }

    private MailClient mailClient(Vertx vertx, MailConfig config, TlsConfig tlsConfig) {
        io.vertx.ext.mail.MailConfig cfg = toVertxMailConfig(config, tlsConfig);
        return MailClient.createShared(vertx, cfg);
    }

    private io.vertx.ext.mail.MailConfig toVertxMailConfig(MailConfig config, TlsConfig tlsConfig) {
        io.vertx.ext.mail.MailConfig cfg = new io.vertx.ext.mail.MailConfig();
        if (config.authMethods.isPresent()) {
            cfg.setAuthMethods(config.authMethods.get());
        }
        cfg.setDisableEsmtp(config.disableEsmtp);
        cfg.setHostname(config.host);
        cfg.setKeepAlive(config.keepAlive);
        if (config.keyStore.isPresent()) {
            JksOptions options = new JksOptions();
            options.setPath(config.keyStore.get());
            if (config.keyStorePassword.isPresent()) {
                options.setPassword(config.keyStorePassword.get());
            }
            cfg.setTrustOptions(options);
        }

        if (config.trustStore.isPresent()) {
            JksOptions options = new JksOptions();
            options.setPath(config.trustStore.get());
            if (config.trustStorePassword.isPresent()) {
                options.setPassword(config.trustStorePassword.get());
            }
            cfg.setTrustOptions(options);
        }

        cfg.setLogin(LoginOption.valueOf(config.login.toUpperCase()));
        cfg.setMaxPoolSize(config.maxPoolSize);
        if (config.ownHostName.isPresent()) {
            cfg.setOwnHostname(config.ownHostName.get());
        }
        if (config.username.isPresent()) {
            cfg.setUsername(config.username.get());
        }
        if (config.password.isPresent()) {
            cfg.setPassword(config.password.get());
        }
        if (config.port.isPresent()) {
            cfg.setPort(config.port.getAsInt());
        }
        if (config.dkim.isPresent()) {
            cfg.setEnableDKIM(true);
            cfg.setDKIMSignOption(createDkimSignOption(config.dkim.get()));
        }

        cfg.setPipelining(config.pipelining);

        cfg.setPoolCleanerPeriod((int) config.poolCleanerPeriod.toMillis());
        cfg.setPoolCleanerPeriodUnit(TimeUnit.MILLISECONDS);

        cfg.setKeepAliveTimeout((int) config.keepAliveTimeout.toMillis());
        cfg.setKeepAliveTimeoutUnit(TimeUnit.MILLISECONDS);

        cfg.setSsl(config.ssl);
        cfg.setStarttls(StartTLSOptions.valueOf(config.startTLS.toUpperCase()));
        cfg.setMultiPartOnly(config.multiPartOnly);
        boolean trustAll = config.trustAll.isPresent() ? config.trustAll.get() : tlsConfig.trustAll;
        cfg.setTrustAll(trustAll);
        return cfg;
    }

    private DKIMSignOptions createDkimSignOption(DKIMSignConfig dkimSignConfig) {
        DKIMSignOptions options = new DKIMSignOptions();
        if (dkimSignConfig.auid.isPresent()) {
            options.setAuid(dkimSignConfig.auid.get());
        }

        if (dkimSignConfig.privateKeyPath.isPresent()) {
            options.setPrivateKeyPath(dkimSignConfig.privateKeyPath.get());
        }

        if (dkimSignConfig.privateKey.isPresent()) {
            options.setPrivateKey(dkimSignConfig.privateKey.get());
        }

        if (dkimSignConfig.signAlgo.equalsIgnoreCase("rsa-sha256")) {
            options.setSignAlgo(DKIMSignAlgorithm.RSA_SHA256);
        } else if (dkimSignConfig.signAlgo.equalsIgnoreCase("rsa-sha1")) {
            options.setSignAlgo(DKIMSignAlgorithm.RSA_SHA1);
        } else {
            throw new IllegalArgumentException("Invalid sign algorithm: " + dkimSignConfig.signAlgo);
        }

        if (dkimSignConfig.signedHeaders.isPresent()) {
            options.setSignedHeaders(toList(dkimSignConfig.signedHeaders.get()));
        } else {
            options.setSignedHeaders(DEFAULT_HEADERS);
        }

        if (dkimSignConfig.sdid.isPresent()) {
            options.setSdid(dkimSignConfig.sdid.get());
        }

        if (dkimSignConfig.auid.isPresent()) {
            options.setAuid(dkimSignConfig.auid.get());
        }

        if (dkimSignConfig.selector.isPresent()) {
            options.setSelector(dkimSignConfig.selector.get());
        }

        if (dkimSignConfig.headerCanonAlgo.isPresent()) {
            options.setHeaderCanonAlgo(CanonicalizationAlgorithm.valueOf(dkimSignConfig.headerCanonAlgo.get().toUpperCase()));
        }

        if (dkimSignConfig.bodyCanonAlgo.isPresent()) {
            options.setBodyCanonAlgo(CanonicalizationAlgorithm.valueOf(dkimSignConfig.bodyCanonAlgo.get().toUpperCase()));
        }

        if (dkimSignConfig.bodyLimit.isPresent()) {
            options.setBodyLimit(dkimSignConfig.bodyLimit.getAsInt());
        }

        options.setSignatureTimestamp(dkimSignConfig.enableSignatureTimestamp);

        if (dkimSignConfig.expirationTime.isPresent()) {
            options.setExpireTime((int) dkimSignConfig.expirationTime.get().toSeconds());
        }

        if (dkimSignConfig.copiedHeaders.isPresent()) {
            options.setCopiedHeaders(toList(dkimSignConfig.copiedHeaders.get()));
        }

        return options;
    }

    private static List<String> toList(String csv) {
        String[] strings = csv.split(",");
        List<String> list = new ArrayList<>(strings.length);
        for (String string : strings) {
            list.add(string.trim());
        }
        return list;
    }

    private static final List<String> DEFAULT_HEADERS = new ArrayList<>();
    static {
        DEFAULT_HEADERS.add("From");
        DEFAULT_HEADERS.add("Reply-to");
        DEFAULT_HEADERS.add("Subject");
        DEFAULT_HEADERS.add("Date");
        DEFAULT_HEADERS.add("To");
        DEFAULT_HEADERS.add("Cc");
        DEFAULT_HEADERS.add("Content-Type");
        DEFAULT_HEADERS.add("Message-ID");
    }

}
