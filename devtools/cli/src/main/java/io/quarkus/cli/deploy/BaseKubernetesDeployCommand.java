package io.quarkus.cli.deploy;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.cli.BuildToolContext;
import io.quarkus.cli.BuildToolDelegatingCommand;
import io.quarkus.cli.Deploy;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

public class BaseKubernetesDeployCommand extends BuildToolDelegatingCommand {

    static final String QUARKUS_DEPLOY_FORMAT = "quarkus.%s.deploy";
    static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";
    static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";
    static final String DEFAULT_IMAGE_BUILDER = "docker";

    @CommandLine.ArgGroup(order = 2, exclusive = false, validate = false, heading = "%nKubernetes options:%n")
    KubernetesOptions kubernetesOptions = new KubernetesOptions();

    @ParentCommand
    Deploy parent;

    @Override
    public Optional<BuildToolDelegatingCommand> getParentCommand() {
        return Optional.of(parent);
    }

    @Override
    public void populateContext(BuildToolContext context) {
        Map<String, String> properties = context.getPropertiesOptions().properties;
        kubernetesOptions.masterUrl.ifPresent(u -> properties.put("quarkus.kubernetes-client.api-server-url", u));
        kubernetesOptions.username.ifPresent(u -> properties.put("quarkus.kubernetes-client.username", u));
        kubernetesOptions.password.ifPresent(p -> properties.put("quarkus.kubernetes-client.password", p));
        kubernetesOptions.token.ifPresent(t -> properties.put("quarkus.kubernetes-client.token", t));
        kubernetesOptions.namespace.ifPresent(n -> properties.put("quarkus.kubernetes-client.namespace", n));

        kubernetesOptions.caCertFile.ifPresent(c -> properties.put("quarkus.kubernetes-client.ca-cert-file", c));
        kubernetesOptions.caCertData.ifPresent(c -> properties.put("quarkus.kubernetes-client.ca-cert-data", c));

        kubernetesOptions.clientCertFile
                .ifPresent(c -> properties.put("quarkus.kubernetes-client.client-cert-file", c));
        kubernetesOptions.clientCertData
                .ifPresent(c -> properties.put("quarkus.kubernetes-client.client-cert-data", c));

        kubernetesOptions.clientKeyFile.ifPresent(c -> properties.put("quarkus.kubernetes-client.client-key-file", c));
        kubernetesOptions.clientKeyData.ifPresent(c -> properties.put("quarkus.kubernetes-client.client-key-data", c));
        kubernetesOptions.clientKeyAlgo.ifPresent(c -> properties.put("quarkus.kubernetes-client.client-key-algo", c));
        kubernetesOptions.clientKeyPassphrase
                .ifPresent(c -> properties.put("quarkus.kubernetes-client.client-key-passphrase", c));

        kubernetesOptions.httpProxy.ifPresent(p -> properties.put("quarkus.kubernetes-client.http-proxy", p));
        kubernetesOptions.httpsProxy.ifPresent(p -> properties.put("quarkus.kubernetes-client.https-proxy", p));
        kubernetesOptions.proxyUsername.ifPresent(p -> properties.put("quarkus.kubernetes-client.proxy-username", p));
        kubernetesOptions.proxyPassword.ifPresent(p -> properties.put("quarkus.kubernetes-client.proxy-password", p));
        if (kubernetesOptions.noProxy != null && kubernetesOptions.noProxy.length > 0) {
            properties.put("quarkus.kubernetes-client.no-proxy",
                    Arrays.stream(kubernetesOptions.noProxy).collect(Collectors.joining(", ")));
        }
        properties.put(QUARKUS_CONTAINER_IMAGE_BUILD, String.valueOf(kubernetesOptions.imageBuilder.isPresent()));

        kubernetesOptions.imageBuilder.or(implicitImageBuilder()).ifPresent(builder -> {
            properties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
            properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, builder);
        });
    }

    @Override
    public void prepareGradle(BuildToolContext context) {
        super.prepareGradle(context);
        Map<String, String> properties = context.getPropertiesOptions().properties;

        Optional<String> builder = Optional.ofNullable(properties.remove(QUARKUS_CONTAINER_IMAGE_BUILDER));
        builder.or(implicitImageBuilder()).ifPresent(b -> {
            context.getParams().add("--image-builder=" + b);
        });
        boolean imageBuild = Optional.ofNullable(properties.remove(QUARKUS_CONTAINER_IMAGE_BUILD))
                .map(Boolean::parseBoolean).orElse(false);
        if (imageBuild) {
            context.getParams().add("--image-build");
        }
    }

    public Supplier<Optional<String>> implicitImageBuilder() {
        return () -> kubernetesOptions.imageBuild ? Optional.of(getDefaultImageBuilder()) : Optional.empty();
    }

    public String getDefaultImageBuilder() {
        return DEFAULT_IMAGE_BUILDER;
    }
}
