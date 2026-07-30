package io.quarkus.kubernetes.client.test.utils;

import java.io.Closeable;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.DevServicesContext;

/**
 * Declare this as a field on a {@code @QuarkusIntegrationTest} (including native tests) to get a
 * {@link KubernetesClient} pointing at the Kubernetes Client Dev Services cluster, without having to
 * wire up a {@link DevServicesContext} field and a {@code @BeforeEach} method manually:
 *
 * <pre>
 * final KubernetesClientDevServicesTestHelper k8s = new KubernetesClientDevServicesTestHelper();
 * </pre>
 *
 * The client obtained through {@link #getClient()} is closed together with this helper, so make sure to call
 * {@link #close()} once done with it, e.g. from an {@code @AfterEach} or {@code @AfterAll} method.
 */
public class KubernetesClientDevServicesTestHelper implements DevServicesContext.ContextAware, Closeable {

    private DevServicesContext context;
    private KubernetesClient client;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }

    public synchronized KubernetesClient getClient() {
        if (client == null) {
            client = KubernetesClientDevServicesUtils.createClient(context);
        }
        return client;
    }

    @Override
    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
