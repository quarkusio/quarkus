package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * @formatter:off
 * Schema used:
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata-schema-v1.2.0.json</a>
 * Notes on proper testing: At least integration-tests modules awt, main, hibernate-orm-panache.
 * @formatter:on
 */
public class NativeImageProxyConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateProxyConfig(BuildProducer<GeneratedResourceBuildItem> proxyConfig,
            List<NativeImageProxyDefinitionBuildItem> proxies) {

        if (proxies.isEmpty()) {
            return;
        }

        try (StringWriter writer = new StringWriter()) {
            // We use sorted array so as the order of elements remains the same between builds.
            final JsonArrayBuilder reflectionArray = Json.sortedArray();
            for (NativeImageProxyDefinitionBuildItem proxy : proxies) {
                // The order of interfaces for a dynamic proxy class is significant:
                // https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
                final JsonArrayBuilder interfaces = Json.array();
                interfaces.addAll(proxy.getClasses());
                final JsonObjectBuilder proxyTypeObj = Json.object();
                proxyTypeObj.put("proxy", interfaces);
                final JsonObjectBuilder reflectionEntry = Json.object();
                reflectionEntry.put("type", proxyTypeObj);
                reflectionArray.add(reflectionEntry);
            }
            final JsonObjectBuilder root = Json.object();
            root.put("reflection", reflectionArray);
            root.appendTo(writer);
            proxyConfig.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/proxy/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
