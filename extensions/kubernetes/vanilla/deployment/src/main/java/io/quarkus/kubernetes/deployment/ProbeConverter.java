
package io.quarkus.kubernetes.deployment;

import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.kubernetes.config.Probe;
import io.dekorate.kubernetes.config.ProbeBuilder;

public class ProbeConverter {

    public static Probe convert(String name, ProbeConfig probe) {
        return builder(name, probe).build();
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
        return ConfigProvider.getConfig().getOptionalValue("quarkus.grpc.server.port", Integer.class)
                .orElse(9000);
    }
}
