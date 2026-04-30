
package io.quarkus.kubernetes.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.kubernetes.config.Probe;
import io.dekorate.kubernetes.config.ProbeBuilder;

public class ProbeConverter {

    public static Probe convert(String name, ProbeConfig probe) {
        return builder(name, probe).build();
    }

    public static io.fabric8.kubernetes.api.model.Probe toKubeProbe(String name, ProbeConfig probe) {
        final var b = new io.fabric8.kubernetes.api.model.ProbeBuilder();
        probe.httpActionPath().ifPresent(path -> b.withNewHttpGet().withPath(path).endHttpGet());
        probe.execAction().ifPresent(cmd -> b.withNewExec().withCommand(cmd).endExec());
        probe.tcpSocketAction().ifPresent(socket -> {
            var hostAndPort = HostAndPort.from(socket);
            b.withNewTcpSocket().withHost(hostAndPort.host).withNewPort(hostAndPort.port).endTcpSocket();
        });
        if (probe.grpcAction().isPresent()) {
            b.withNewGrpc().withPort(Integer.parseInt(probe.grpcAction().get())).endGrpc();
        } else if (probe.grpcActionEnabled()) {
            b.withNewGrpc().withPort(getQuarkusGrpcPort()).withService(name).endGrpc();
        }

        b.withInitialDelaySeconds((int) probe.initialDelay().getSeconds());
        b.withPeriodSeconds((int) probe.period().getSeconds());
        b.withTimeoutSeconds((int) probe.timeout().getSeconds());
        b.withSuccessThreshold(probe.successThreshold());
        b.withFailureThreshold(probe.failureThreshold());
        return b.build();
    }

    private record HostAndPort(String host, String port) {
        static HostAndPort from(String hostAndPort) {
            if (hostAndPort == null || hostAndPort.isEmpty()) {
                return null;
            }
            int colon = hostAndPort.indexOf(':');
            if (colon < 0) {
                return null;
            } else {
                String host = hostAndPort.substring(0, colon);
                String port = hostAndPort.substring(colon + 1);
                return new HostAndPort(host, port);
            }
        }
    }

    public static ProbeBuilder builder(String name, ProbeConfig probe) {
        ProbeBuilder b = new ProbeBuilder();
        probe.httpActionPath().ifPresent(b::withHttpActionPath);
        probe.execAction().ifPresent(b::withExecAction);
        probe.tcpSocketAction().ifPresent(b::withTcpSocketAction);
        if (probe.grpcAction().isPresent()) {
            b.withGrpcAction(probe.grpcAction().get());
        } else if (probe.grpcActionEnabled()) {
            b.withGrpcAction(getQuarkusGrpcPort() + ":" + name);
        }

        b.withInitialDelaySeconds((int) probe.initialDelay().getSeconds());
        b.withPeriodSeconds((int) probe.period().getSeconds());
        b.withTimeoutSeconds((int) probe.timeout().getSeconds());
        b.withSuccessThreshold(probe.successThreshold());
        b.withFailureThreshold(probe.failureThreshold());
        return b;
    }

    private static int getQuarkusGrpcPort() {
        // TODO - Querying a runtime configuration during deployment
        return ConfigProvider.getConfig().getOptionalValue("quarkus.grpc.server.port", Integer.class)
                .orElse(9000);
    }
}
