package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class DisposableNamespaceTestResource extends AbstractNamespaceManagingTestResource
        implements
        QuarkusTestResourceConfigurableLifecycleManager<WithDisposableNamespace> {

    private static final Logger log = LoggerFactory.getLogger(DisposableNamespaceTestResource.class);
    private int waitAtMostSecondsForNSDeletion;

    @Override
    protected Map<String, String> doStart() {
        // override the namespace to be used
        final var systemProps = Map.of(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, namespace());

        //these actually need to be system properties
        //as they are read directly as system props, and not from Quarkus config
        for (Map.Entry<String, String> entry : systemProps.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        return systemProps;
    }

    @Override
    protected void doStop(boolean deletedNamespace) {
        // nothing to do
    }

    @Override
    protected Class<? extends Annotation> relatedAnnotationClass() {
        return WithDisposableNamespace.class;
    }

    @Override
    protected Logger logger() {
        return log;
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(client(),
                new TestInjector.AnnotatedAndMatchesType(DisposableNamespacedKubernetesClient.class, KubernetesClient.class));
    }

    @Override
    public void init(WithDisposableNamespace annotation) {
        initNamespaceAndClient(annotation.namespace());
        waitAtMostSecondsForNSDeletion = annotation.waitAtMostSecondsForDeletion();
        // preserveNamespaceOnError = annotation.preserveOnError(); todo: ns preservation
    }

    @Override
    protected String defaultNamespaceName(KubernetesClient client) {
        return KubernetesResourceUtil.sanitizeName("ns" + UUID.randomUUID());
    }

    @Override
    protected int numberOfSecondsToWaitForNamespaceDeletion() {
        return waitAtMostSecondsForNSDeletion;
    }
}
