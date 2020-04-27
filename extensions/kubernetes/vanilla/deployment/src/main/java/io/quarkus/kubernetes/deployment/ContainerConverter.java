
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.config.ContainerBuilder;

public class ContainerConverter {

    public static Container convert(Map.Entry<String, ContainerConfig> e) {
        return convert(e.getValue()).withName(e.getKey()).build();
    }

    private static ContainerBuilder convert(ContainerConfig c) {
        ContainerBuilder b = new ContainerBuilder();
        c.image.ifPresent(i -> b.withImage(i));
        c.workingDir.ifPresent(w -> b.withWorkingDir(w));
        c.readinessProbe.ifPresent(p -> b.withReadinessProbe(ProbeConverter.convert(p)));
        c.livenessProbe.ifPresent(p -> b.withLivenessProbe(ProbeConverter.convert(p)));
        b.addAllToEnvVars(c.convertToEnvs());
        c.ports.entrySet().forEach(e -> b.addToPorts(PortConverter.convert(e)));
        c.mounts.entrySet().forEach(e -> b.addToMounts(MountConverter.convert(e)));
        return b;
    }
}
