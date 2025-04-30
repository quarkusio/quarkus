package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import io.quarkus.builder.Json;
import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageResourceConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateResourceConfig(BuildProducer<GeneratedResourceBuildItem> resourceConfig,
            List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<NativeImageResourceBuildItem> resources,
            List<ServiceProviderBuildItem> serviceProviderBuildItems) {
        JsonObjectBuilder root = Json.object();

        JsonObjectBuilder resourcesJs = Json.object();
        JsonArrayBuilder includes = Json.array();
        JsonArrayBuilder excludes = Json.array();

        for (NativeImageResourceBuildItem i : resources) {
            for (String path : i.getResources()) {
                JsonObjectBuilder pat = Json.object();
                pat.put("pattern", Pattern.quote(path));
                includes.add(pat);
            }
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            includes.add(Json.object().put("pattern", Pattern.quote(i.serviceDescriptorFile())));
        }

        for (NativeImageResourcePatternsBuildItem resourcePatternsItem : resourcePatterns) {
            addListToJsonArray(includes, resourcePatternsItem.getIncludePatterns());
            addListToJsonArray(excludes, resourcePatternsItem.getExcludePatterns());
        }
        resourcesJs.put("includes", includes);
        resourcesJs.put("excludes", excludes);
        root.put("resources", resourcesJs);

        JsonArrayBuilder bundles = Json.array();
        for (NativeImageResourceBundleBuildItem i : resourceBundles) {
            JsonObjectBuilder bundle = Json.object();
            String moduleName = i.getModuleName();
            StringBuilder sb = new StringBuilder();
            if (moduleName != null) {
                sb.append(moduleName).append(":");
            }
            sb.append(i.getBundleName().replace("/", "."));
            bundle.put("name", sb.toString());
            bundles.add(bundle);
        }
        root.put("bundles", bundles);

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            resourceConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/resource-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addListToJsonArray(JsonArrayBuilder array, List<String> patterns) {
        for (String pattern : patterns) {
            JsonObjectBuilder pat = Json.object();
            pat.put("pattern", pattern);
            array.add(pat);
        }
    }
}
