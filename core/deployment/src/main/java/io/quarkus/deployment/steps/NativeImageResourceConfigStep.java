package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * @formatter:off
 * Schema used:
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata-schema-v1.2.0.json</a>
 * Notes on proper testing: At least integration-tests modules awt, main.
 * @formatter:on
 */
public class NativeImageResourceConfigStep {

    private static final Logger log = Logger.getLogger(NativeImageResourceConfigStep.class);

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateResourceConfig(BuildProducer<GeneratedResourceBuildItem> resourceConfig,
            List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<NativeImageResourceBuildItem> resources,
            List<ServiceProviderBuildItem> serviceProviderBuildItems) {

        final JsonArrayBuilder resourcesArray = Json.array();
        for (NativeImageResourceBuildItem i : resources) {
            for (String path : i.getResources()) {
                resourcesArray.add(Json.object().put("glob", escapeGlob(path)));
            }
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            resourcesArray.add(Json.object().put("glob", escapeGlob(i.serviceDescriptorFile())));
        }

        for (NativeImageResourcePatternsBuildItem i : resourcePatterns) {
            final List<String> globs = i.getIncludeGlobs();
            final String module = i.getModule();

            if (globs != null) {
                for (String glob : globs) {
                    final JsonObjectBuilder globObj = Json.object().put("glob", glob);
                    if (module != null && !module.isEmpty()) {
                        globObj.put("module", module);
                    }
                    resourcesArray.add(globObj);
                }
            }
        }

        for (NativeImageResourceBundleBuildItem i : resourceBundles) {
            final String moduleName = i.getModuleName();
            final String bundleName = i.getBundleName().replace("/", ".");
            final String name = (moduleName != null && !moduleName.isEmpty())
                    ? moduleName + ":" + bundleName
                    : bundleName;
            resourcesArray.add(Json.object().put("bundle", name));
        }

        if (resourcesArray.isEmpty()) {
            return;
        }

        final JsonObjectBuilder root = Json.object().put("resources", resourcesArray);
        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            resourceConfig.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/resource/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeGlob(String path) {
        return path.replace("*", "\\*");
    }
}
