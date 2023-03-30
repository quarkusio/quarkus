package io.quarkus.openshift.runtime.devui;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.dev.console.DevConsoleManager;

@ApplicationScoped
public class OpenshiftDeploymentJsonRpcService {

    public String build(String type, Boolean expose, Boolean untrusted) {
        Map<String, String> params = Map.of(
                "quarkus.openshift.route.expose", expose.toString(),
                "quarkus.kubernetes-client.trust-certs", untrusted.toString(),
                "quarkus.build.package-type", type);

        return DevConsoleManager.invoke("openshift-deployment-action", params);
    }

}
