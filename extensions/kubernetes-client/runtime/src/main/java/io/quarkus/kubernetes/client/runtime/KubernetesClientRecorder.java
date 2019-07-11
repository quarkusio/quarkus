package io.quarkus.kubernetes.client.runtime;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesClientRecorder {

    public BeanContainerListener setBuildConfig(KubernetesClientBuildConfig buildConfig) {
        return beanContainer -> {
            KubernetesClientProducer producer = beanContainer.instance(KubernetesClientProducer.class);
            producer.setKubernetesClientBuildConfig(buildConfig);
        };
    }
}
