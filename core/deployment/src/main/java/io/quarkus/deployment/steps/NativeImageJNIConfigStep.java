package io.quarkus.deployment.steps;

import static io.quarkus.deployment.steps.NativeImageFFMConfigStep.isGraalVm25OrNewer;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * Schema used:
 * https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json
 */
public class NativeImageJNIConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateJniConfig(BuildProducer<GeneratedResourceBuildItem> jniConfig,
            List<JniRuntimeAccessBuildItem> jniRuntimeAccessibleClasses,
            List<JniRuntimeAccessFieldBuildItem> jniRuntimeAccessibleFields,
            List<JniRuntimeAccessMethodBuildItem> jniRuntimeAccessibleMethods,
            NativeImageRunnerBuildItem nativeImageRunnerBuildItem) {

        if (jniRuntimeAccessibleClasses.isEmpty() && jniRuntimeAccessibleFields.isEmpty()
                && jniRuntimeAccessibleMethods.isEmpty()) {
            return;
        }

        final Map<String, JniInfo> jniClasses = new LinkedHashMap<>();

        jniRuntimeAccessibleClasses.forEach(c -> addJniClass(jniClasses, c));
        jniRuntimeAccessibleFields.forEach(f -> addJniField(jniClasses, f));
        jniRuntimeAccessibleMethods.forEach(m -> addJniMethod(jniClasses, m));

        final JsonArrayBuilder reflectionArray = Json.array();

        for (Map.Entry<String, JniInfo> entry : jniClasses.entrySet()) {
            final JsonObjectBuilder json = Json.object();
            json.put("type", entry.getKey());
            json.put("jniAccessible", true);
            final JniInfo info = entry.getValue();
            final JsonArrayBuilder methodsArray = Json.array();
            if (info.constructors) {
                json.put("allDeclaredConstructors", true);
            } else if (!info.ctorSet.isEmpty()) {
                for (JniRuntimeAccessMethodBuildItem ctor : info.ctorSet) {
                    final JsonObjectBuilder methodObject = Json.object();
                    methodObject.put("name", ctor.getName());
                    final JsonArrayBuilder paramsArray = Json.array();
                    Collections.addAll(paramsArray, ctor.getParams());
                    methodObject.put("parameterTypes", paramsArray);
                    methodsArray.add(methodObject);
                }
            }

            if (info.methods) {
                json.put("allDeclaredMethods", true);
            } else if (!info.methodSet.isEmpty()) {
                for (JniRuntimeAccessMethodBuildItem method : info.methodSet) {
                    final JsonObjectBuilder methodObject = Json.object();
                    methodObject.put("name", method.getName());
                    final JsonArrayBuilder paramsArray = Json.array();
                    paramsArray.addAll(Arrays.asList(method.getParams()));
                    methodObject.put("parameterTypes", paramsArray);
                    methodsArray.add(methodObject);
                }
            }

            if (!methodsArray.isEmpty()) {
                json.put("methods", methodsArray);
            }

            if (info.fields) {
                json.put("allDeclaredFields", true);
            } else if (!info.fieldSet.isEmpty()) {
                final JsonArrayBuilder fieldsArray = Json.array();
                for (String fieldName : info.fieldSet) {
                    fieldsArray.add(Json.object().put("name", fieldName));
                }
                json.put("fields", fieldsArray);
            }

            reflectionArray.add(json);
        }
        final boolean isGraalVm25OrNewer = isGraalVm25OrNewer(nativeImageRunnerBuildItem);
        final JsonObjectBuilder root = Json.object();
        if (isGraalVm25OrNewer) {
            root.put("reflection", reflectionArray);
        } else {
            // GraalVM/Mandrel 21 uses the "jni" key for JNI metadata in reachability-metadata.json
            root.put("jni", reflectionArray);
        }

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            jniConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/jni/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
            if (!isGraalVm25OrNewer) {
                // forces GraalVM/Mandrel 21 to locate the file
                jniConfig.produce(new GeneratedResourceBuildItem(
                        "META-INF/native-image/jni/native-image.properties",
                        "Args = -H:+UnlockExperimentalVMOptions -H:ConfigurationResourceRoots=META-INF/native-image/jni/ -H:-UnlockExperimentalVMOptions\n"
                                .getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addJniClass(Map<String, JniInfo> jniClasses, JniRuntimeAccessBuildItem jniRuntimeAccessBuildItem) {
        for (String cl : jniRuntimeAccessBuildItem.getClassNames()) {
            final JniInfo existing = jniClasses.get(cl);
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

    private void addJniMethod(Map<String, JniInfo> reflectiveClasses, JniRuntimeAccessMethodBuildItem methodInfo) {
        final String cl = methodInfo.getDeclaringClass();
        JniInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new JniInfo());
        }
        if (methodInfo.getName().equals("<init>")) {
            existing.ctorSet.add(methodInfo);
        } else {
            existing.methodSet.add(methodInfo);
        }
    }

    private void addJniField(Map<String, JniInfo> jniClasses, JniRuntimeAccessFieldBuildItem fieldInfo) {
        final String cl = fieldInfo.getDeclaringClass();
        JniInfo existing = jniClasses.get(cl);
        if (existing == null) {
            jniClasses.put(cl, existing = new JniInfo());
        }
        existing.fieldSet.add(fieldInfo.getName());
    }

    static final class JniInfo {
        boolean constructors;
        boolean methods;
        boolean fields;
        final Set<String> fieldSet = new HashSet<>();
        final Set<JniRuntimeAccessMethodBuildItem> methodSet = new HashSet<>();
        final Set<JniRuntimeAccessMethodBuildItem> ctorSet = new HashSet<>();

        private JniInfo(boolean methods, boolean fields, boolean constructors) {
            this.methods = methods;
            this.fields = fields;
            this.constructors = constructors;
        }

        private JniInfo() {
            this(false, false, false);
        }

        private JniInfo(JniRuntimeAccessBuildItem jniRuntimeAccessBuildItem) {
            this(jniRuntimeAccessBuildItem.isMethods(),
                    jniRuntimeAccessBuildItem.isFields(),
                    jniRuntimeAccessBuildItem.isConstructors());
        }
    }
}
