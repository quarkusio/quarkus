
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Probe;
import io.dekorate.kubernetes.config.ProbeBuilder;

public class ProbeConverter {

    public static Probe convert(ProbeConfig probe) {
        ProbeBuilder b = new ProbeBuilder();
        probe.httpActionPath.ifPresent(v -> b.withHttpActionPath(v));
        probe.execAction.ifPresent(v -> b.withExecAction(v));
        probe.tcpSocketAction.ifPresent(v -> b.withTcpSocketAction(v));
        b.withInitialDelaySeconds(probe.initialDelaySeconds);
        b.withPeriodSeconds(probe.periodSeconds);
        b.withTimeoutSeconds(probe.timeoutSeconds);
        b.withSuccessThreshold(probe.successThreshold);
        b.withFailureThreshold(probe.failureThreshold);
        return b.build();
    }
}
