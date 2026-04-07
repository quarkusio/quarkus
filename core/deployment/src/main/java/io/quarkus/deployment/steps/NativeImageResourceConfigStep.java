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
 * Schema used:
 * https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json
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
            final List<String> legacyRegexes = i.getIncludePatterns();
            if (legacyRegexes != null && !legacyRegexes.isEmpty()) {
                log.errorf("GraalVM reachability-metadata no longer supports Regex resource patterns. "
                        + "Extension must migrate from .includePattern() to .includeGlob(). Offending regexes: "
                        + legacyRegexes);
            }

            final List<String> globs = i.getIncludeGlobs();
            final String module = i.getModule();

            if (globs != null) {
                for (String glob : globs) {
                    final JsonObjectBuilder globObj = Json.object().put("glob", glob);
                    // wire up the new module support to the JSON output for resources buried in modules
                    if (module != null && !module.isEmpty()) {
                        globObj.put("module", module);
                    }
                    resourcesArray.add(globObj);
                }
            }

            final List<String> excludes = i.getExcludePatterns();
            if (excludes != null && !excludes.isEmpty()) {
                log.warnf("Resource excludes are not supported in GraalVM reachability-metadata. "
                        + "Ignored these exclude patterns: %s", excludes);
            }
        }

        for (NativeImageResourceBundleBuildItem i : resourceBundles) {
            final String moduleName = i.getModuleName();
            final String bundleName = i.getBundleName().replace("/", ".");
            // if module is present, "moduleName:bundleName" inside the "bundle" string is expected
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
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String escapeGlob(final String path) {
        return path.replace("*", "\\*");
    }
}
