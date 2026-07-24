package io.quarkus.kubernetes.client.test.utils;

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
 */
public class KubernetesClientDevServicesTestHelper implements DevServicesContext.ContextAware {

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
}
