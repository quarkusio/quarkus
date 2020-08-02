
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
        c.image.ifPresent(b::withImage);
        c.workingDir.ifPresent(b::withWorkingDir);
        c.command.ifPresent(w -> b.withCommand(w.toArray(new String[0])));
        c.arguments.ifPresent(w -> b.withArguments(w.toArray(new String[0])));
        c.readinessProbe.ifPresent(p -> b.withReadinessProbe(ProbeConverter.convert(p)));
        c.livenessProbe.ifPresent(p -> b.withLivenessProbe(ProbeConverter.convert(p)));
        b.addAllToEnvVars(c.convertToEnvs());
        c.ports.entrySet().forEach(e -> b.addToPorts(PortConverter.convert(e)));
        c.mounts.entrySet().forEach(e -> b.addToMounts(MountConverter.convert(e)));
        return b;
    }
}
