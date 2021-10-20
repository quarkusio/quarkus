
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.config.ContainerBuilder;

public class ContainerConverter {

    public static Container convert(Map.Entry<String, ContainerConfig> e) {
        return convert(e.getKey(), e.getValue()).build();
    }

    private static ContainerBuilder convert(String name, ContainerConfig c) {
        ContainerBuilder b = new ContainerBuilder();
        b.withName(name);
        c.image.ifPresent(b::withImage);
        c.workingDir.ifPresent(b::withWorkingDir);
        c.command.ifPresent(w -> b.withCommand(w.toArray(new String[0])));
        c.arguments.ifPresent(w -> b.withArguments(w.toArray(new String[0])));
        if (c.readinessProbe != null && c.readinessProbe.hasUserSuppliedAction()) {
            b.withReadinessProbe(ProbeConverter.convert(c.readinessProbe));
        }
        if (c.livenessProbe != null && c.livenessProbe.hasUserSuppliedAction()) {
            b.withLivenessProbe(ProbeConverter.convert(c.livenessProbe));
        }
        b.addAllToEnvVars(c.convertToEnvs());
        c.ports.entrySet().forEach(e -> b.addToPorts(PortConverter.convert(e)));
        c.mounts.entrySet().forEach(e -> b.addToMounts(MountConverter.convert(e)));

        if (c.resources.requests.memory.isPresent() || c.resources.requests.cpu.isPresent()) {
            b.withNewRequestResources(c.resources.requests.memory.orElse(null), c.resources.requests.cpu.orElse(null));
        }

        if (c.resources.limits.memory.isPresent() || c.resources.limits.cpu.isPresent()) {
            b.withNewLimitResources(c.resources.limits.memory.orElse(null), c.resources.limits.cpu.orElse(null));
        }
        return b;
    }
}
