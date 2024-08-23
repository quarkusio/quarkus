package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.quarkus.builder.Json;
import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageProxyConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateProxyConfig(BuildProducer<GeneratedResourceBuildItem> proxyConfig,
            List<NativeImageProxyDefinitionBuildItem> proxies) {
        JsonArrayBuilder root = Json.array();

        for (NativeImageProxyDefinitionBuildItem proxy : proxies) {
            JsonArrayBuilder interfaces = Json.array();
            for (String cl : proxy.getClasses()) {
                interfaces.add(cl);
            }
            JsonObjectBuilder proxyJson = Json.object();
            proxyJson.put("interfaces", interfaces);
            root.add(proxyJson);
        }

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            proxyConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/proxy-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
