package io.quarkus.vault.runtime.config;

public enum VaultAuthenticationType {

    /**
     * Kubernetes vault authentication
     * <p>
     * When running the application into kubernetes, it is possible to benefit from the vault/kubernetes integration
     * for authentication. Once the kubernetes authentication has been enabled into vault, create a vault role
     * associating one or more vault policies, with one or more service accounts and one or more namespaces.
     * When selecting the kubernetes authentication type, specify the vault authentication role to use.
     * <p>
     * see https://www.vaultproject.io/api/auth/kubernetes/index.html
     */
    KUBERNETES,

    /**
     * Username & password vault authentication
     * <p>
     * Vault supports authentication through username and password. Before using it, userpass auth needs to be enabled
     * in vault, and a user needs to be created with his password. This type of authentication is easy to use,
     * and useful in development environments. When using in production, some care will have to be taken to keep
     * some confidentiality on the password.
     * <p>
     * https://www.vaultproject.io/api/auth/userpass/index.html
     */
    USERPASS,

    /**
     * Role & secret vault authentication using AppRole method
     * <p>
     * <p>
     * https://www.vaultproject.io/api/auth/approle/index.html
     */
    APPROLE

}
