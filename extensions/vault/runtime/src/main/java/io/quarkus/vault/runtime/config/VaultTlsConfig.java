package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_TLS_SKIP_VERIFY;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_TLS_USE_KUBERNETES_CACERT;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultTlsConfig {

    /**
     * Allows to bypass certificate validation on TLS communications.
     * <p>
     * If true this will allow TLS communications with Vault, without checking the validity of the
     * certificate presented by Vault. This is discouraged in production because it allows man in the middle
     * type of attacks.
     */
    @ConfigItem(defaultValue = DEFAULT_TLS_SKIP_VERIFY)
    public boolean skipVerify;

    /**
     * Certificate bundle used to validate TLS communications with Vault.
     * <p>
     * The path to a pem bundle file, if TLS is required, and trusted certificates are not set through
     * javax.net.ssl.trustStore system property.
     */
    @ConfigItem
    public Optional<String> caCert;

    /**
     * If true and Vault authentication type is kubernetes, TLS will be active and the cacert path will
     * be set to /var/run/secrets/kubernetes.io/serviceaccount/ca.crt. If set, this setting will take precedence
     * over property quarkus.vault.tls.ca-cert. This means that if Vault authentication type is kubernetes
     * and we want to use quarkus.vault.tls.ca-cert or system property javax.net.ssl.trustStore, then this
     * property should be set to false.
     */
    @ConfigItem(defaultValue = DEFAULT_TLS_USE_KUBERNETES_CACERT)
    public boolean useKubernetesCaCert;

}
