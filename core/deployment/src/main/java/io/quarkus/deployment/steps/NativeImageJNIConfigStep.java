package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.builder.Json;
import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageJNIConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateJniConfig(BuildProducer<GeneratedResourceBuildItem> jniConfig,
            List<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses) {
        final Map<String, JniInfo> jniClasses = new LinkedHashMap<>();

        for (JniRuntimeAccessBuildItem jniAccessible : jniRuntimeAccessibleClasses) {
            addJniClass(jniClasses, jniAccessible);
        }

        JsonArrayBuilder root = Json.array();
        for (Map.Entry<String, JniInfo> entry : jniClasses.entrySet()) {
            JsonObjectBuilder json = Json.object();

            json.put("name", entry.getKey());

            JniInfo info = entry.getValue();
            if (info.constructors) {
                json.put("allDeclaredConstructors", true);
            }
            if (info.methods) {
                json.put("allDeclaredMethods", true);
            }
            if (info.fields) {
                json.put("allDeclaredFields", true);
            }

            root.add(json);
        }

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            jniConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/jni-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addJniClass(Map<String, JniInfo> jniClasses, JniRuntimeAccessBuildItem jniRuntimeAccessBuildItem) {
        for (String cl : jniRuntimeAccessBuildItem.getClassNames()) {
            JniInfo existing = jniClasses.get(cl);
            if (existing == null) {
                jniClasses.put(cl, new JniInfo(jniRuntimeAccessBuildItem));
            } else {
                if (jniRuntimeAccessBuildItem.isConstructors()) {
                    existing.constructors = true;
                }
                if (jniRuntimeAccessBuildItem.isMethods()) {
                    existing.methods = true;
                }
                if (jniRuntimeAccessBuildItem.isFields()) {
                    existing.fields = true;
                }
            }
        }
    }

    static final class JniInfo {
        boolean constructors;
        boolean methods;
        boolean fields;

        private JniInfo(JniRuntimeAccessBuildItem jniRuntimeAccessBuildItem) {
            this.methods = jniRuntimeAccessBuildItem.isMethods();
            this.fields = jniRuntimeAccessBuildItem.isFields();
            this.constructors = jniRuntimeAccessBuildItem.isConstructors();
        }
    }

}
