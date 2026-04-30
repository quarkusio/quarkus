package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.config.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.Quantity;

public class ContainerConverter {

    public static Container convert(Map.Entry<String, ContainerConfig> e) {
        return convert(e.getKey(), e.getValue()).build();
    }

    private static ContainerBuilder convert(String name, ContainerConfig c) {
        ContainerBuilder b = new ContainerBuilder();
        b.withName(name);
        b.withImagePullPolicy(c.imagePullPolicy());
        c.image().ifPresent(b::withImage);
        c.workingDir().ifPresent(b::withWorkingDir);
        c.command().ifPresent(w -> b.withCommand(w.toArray(new String[0])));
        c.arguments().ifPresent(w -> b.withArguments(w.toArray(new String[0])));
        if (c.readinessProbe() != null && c.readinessProbe().hasUserSuppliedAction()) {
            b.withReadinessProbe(ProbeConverter.convert(name, c.readinessProbe()));
        }
        if (c.livenessProbe() != null && c.livenessProbe().hasUserSuppliedAction()) {
            b.withLivenessProbe(ProbeConverter.convert(name, c.livenessProbe()));
        }
        b.addAllToEnvVars(c.convertToEnvs());
        c.ports().entrySet().forEach(e -> b.addToPorts(PortConverter.convert(e)));
        c.mounts().entrySet().forEach(e -> b.addToMounts(MountConverter.convert(e)));

        if (c.resources().requests().memory().isPresent() || c.resources().requests().cpu().isPresent()) {
            b.withNewRequestResources(c.resources().requests().memory().orElse(null),
                    c.resources().requests().cpu().orElse(null));
        }

        if (c.resources().limits().memory().isPresent() || c.resources().limits().cpu().isPresent()) {
            b.withNewLimitResources(c.resources().limits().memory().orElse(null), c.resources().limits().cpu().orElse(null));
        }
        return b;
    }

    private static final String CPU = "cpu";
    private static final String MEMORY = "memory";

    public static io.fabric8.kubernetes.api.model.Container toKubeContainer(Map.Entry<String, ContainerConfig> e) {
        final var b = new io.fabric8.kubernetes.api.model.ContainerBuilder();
        final var name = e.getKey();
        b.withName(name);
        final var c = e.getValue();
        b.withImagePullPolicy(c.imagePullPolicy().name());
        c.image().ifPresent(b::withImage);
        c.workingDir().ifPresent(b::withWorkingDir);
        c.command().ifPresent(b::withCommand);
        c.arguments().ifPresent(b::withArgs);
        if (c.readinessProbe() != null && c.readinessProbe().hasUserSuppliedAction()) {
            b.withReadinessProbe(ProbeConverter.toKubeProbe(name, c.readinessProbe()));
        }
        if (c.livenessProbe() != null && c.livenessProbe().hasUserSuppliedAction()) {
            b.withLivenessProbe(ProbeConverter.toKubeProbe(name, c.livenessProbe()));
        }
        b.addAllToEnv(c.getEnvVars());
        b.addAllToEnvFrom(c.getEnvFroms());
        b.addAllToPorts(c.ports().entrySet().stream().map(PortConverter::toKubeContainerPort).toList());
        b.addAllToVolumeMounts(c.mounts().entrySet().stream().map(MountConverter::toVolumeMount).toList());

        setLimitsAndRequests(c.resources(), b);

        return b.build();
    }

    public static void setLimitsAndRequests(ResourcesConfig c, io.fabric8.kubernetes.api.model.ContainerFluent<?> b) {
        final var requests = c.requests();
        var mem = requests.memory().orElse(null);
        var cpu = requests.cpu().orElse(null);
        if (mem != null || cpu != null) {
            final var resources = b.withNewResources();
            if (mem != null) {
                resources.addToRequests(MEMORY, new Quantity(mem));
            }
            if (cpu != null) {
                resources.addToRequests(CPU, new Quantity(cpu));
            }
            resources.endResources();
        }

        final var limits = c.limits();
        mem = limits.memory().orElse(null);
        cpu = limits.cpu().orElse(null);
        if (mem != null || cpu != null) {
            final var resources = b.editOrNewResources();
            if (mem != null) {
                resources.addToLimits(MEMORY, new Quantity(mem));
            }
            if (cpu != null) {
                resources.addToLimits(CPU, new Quantity(cpu));
            }
            resources.endResources();
        }
    }
}
