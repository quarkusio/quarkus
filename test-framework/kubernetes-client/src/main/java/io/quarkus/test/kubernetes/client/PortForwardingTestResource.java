package io.quarkus.test.kubernetes.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;
import io.quarkus.test.kubernetes.client.WithPortForwarding.FieldSelector;
import io.quarkus.test.kubernetes.client.WithPortForwarding.LabelValue;

public class PortForwardingTestResource extends AbstractNamespaceConnectingTestResource
        implements QuarkusTestResourceConfigurableLifecycleManager<WithPortForwarding> {

    private static final Logger log = LoggerFactory.getLogger(PortForwardingTestResource.class);
    private LocalPortForward portForward;
    private String podName;
    private int port;
    private int localPort;

    @Override
    public void init(WithPortForwarding annotation) {
        initNamespaceAndClient(annotation.namespace());

        final var podIdentifier = annotation.pod();

        // identify which pod to target
        Pod pod;
        final var hasLabelSelector = !AnnotationConstants.UNSET_STRING_VALUE.equals(podIdentifier.labelSelector());
        final var hasFieldSelectors = podIdentifier.fieldSelectors().length > 0;
        final var hasLabelValues = podIdentifier.labelValues().length > 0;
        if (!hasLabelSelector && !hasFieldSelectors && !hasLabelValues) {
            throw new IllegalArgumentException(
                    "Must specify label values and/or label and/or field selectors to identify which pod to port forward");
        }

        // retrieve pod name from selectors and optional index
        final var podsResource = client().pods().inNamespace(namespace());
        if (hasLabelSelector) {
            podsResource.withLabelSelector(podIdentifier.labelSelector());
        }
        if (hasFieldSelectors) {
            for (FieldSelector fieldSelector : podIdentifier.fieldSelectors()) {
                switch (fieldSelector.operator()) {
                    case eq:
                        podsResource.withField(fieldSelector.key(), fieldSelector.value());
                        break;
                    case neq:
                        podsResource.withoutField(fieldSelector.key(), fieldSelector.value());
                    default:
                        throw new IllegalArgumentException("Unknown field selector operator: " + fieldSelector.operator());
                }
            }
        }
        if (hasLabelValues) {
            for (LabelValue labelValue : podIdentifier.labelValues()) {
                final var key = labelValue.key();
                final var value = labelValue.value();
                if (AnnotationConstants.UNSET_STRING_VALUE.equals(value)) {
                    podsResource.withLabel(key);
                } else {
                    podsResource.withLabel(key, value);
                }
            }
        }

        final var pods = podsResource.list().getItems();
        final var podsNumber = pods.size();
        final var matchingString = "matching (labels: '"
                + podIdentifier.labelSelector() + "', fields: "
                + Arrays.toString(podIdentifier.fieldSelectors());
        if (podsNumber == 0) {
            throw new IllegalArgumentException("No pod " + matchingString);
        }

        final var podIndex = podIdentifier.podIndex();
        if (podIndex < podsNumber) {
            pod = pods.get(podIndex);
        } else {
            throw new IndexOutOfBoundsException("There are only " + podsNumber
                    + " pods " + matchingString + " but provided index was " + podIndex);
        }

        this.podName = pod.getMetadata().getName();

        // deal with port
        var portInfo = annotation.port();
        port = portInfo.port();
        if (AnnotationConstants.UNSET_INT_VALUE == port) {
            port = pod.getSpec().getContainers()
                    .get(portInfo.containerIndex())
                    .getPorts().get(portInfo.portIndex()).getContainerPort();
        }

        // if local port is not specified, use same as container port
        localPort = annotation.localPort();
        if (AnnotationConstants.UNSET_INT_VALUE == localPort) {
            localPort = port;
        }
    }

    @Override
    public Map<String, String> start() {
        portForward = client().pods().inNamespace(namespace()).withName(podName).portForward(port, localPort);

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        try {
            portForward.close();
        } catch (IOException e) {
            logger().warn("Failed to close port forward: " + e.getMessage());
            // ignore
        }
    }

    @Override
    protected Logger logger() {
        return log;
    }
}
