package io.quarkus.kubernetes.client.deployment;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.logging.Logger;

public class KubernetesClientErrorHandler {

    private static final Logger LOG = Logger.getLogger(KubernetesClientErrorHandler.class);

    public static void handle(Exception e) {
        if (e.getCause() instanceof SSLHandshakeException) {
            LOG.error(
                    "The application could not be deployed to the cluster because the Kubernetes API Server certificates are not trusted. The certificates can be configured using the relevant configuration properties under the 'quarkus.kubernetes-client' config root, or \"quarkus.kubernetes-client.trust-certs=true\" can be set to explicitly trust the certificates (not recommended)");
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }
}
