package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
import io.quarkus.deployment.builditem.nativeimage.ForceNonWeakReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassConditionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

/**
 * @formatter:off
 * Schema used:
 * <a href="https://github.com/graalvm/graalvm-community-jdk25u/blob/master/docs/reference-manual/native-image/assets/reachability-metadata-schema-v1.2.0.json">reachability-metadata-schema-v1.2.0.json</a>
 * Notes on proper testing: At least integration-tests modules awt, main, hibernate-orm-panache, native-image-annotations, rest-client.
 * @formatter:on
 */
public class NativeImageReflectConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateReflectConfig(BuildProducer<GeneratedResourceBuildItem> reflectConfig,
            NativeConfig nativeConfig,
            List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<ForceNonWeakReflectiveClassBuildItem> nonWeakReflectiveClassBuildItems,
            List<ServiceProviderBuildItem> serviceProviderBuildItems,
            List<ReflectiveClassConditionBuildItem> reflectiveClassConditionBuildItems) {

        final Map<String, ReflectionInfo> reflectiveClasses = new LinkedHashMap<>();
        final Set<String> forcedNonWeakClasses = new HashSet<>();

        for (ForceNonWeakReflectiveClassBuildItem nonWeak : nonWeakReflectiveClassBuildItems) {
            forcedNonWeakClasses.add(nonWeak.getClassName());
        }
        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            addReflectiveClass(reflectiveClasses, forcedNonWeakClasses, i);
        }
        for (ReflectiveFieldBuildItem i : reflectiveFields) {
            addReflectiveField(reflectiveClasses, i);
        }
        for (ReflectiveMethodBuildItem i : reflectiveMethods) {
            addReflectiveMethod(reflectiveClasses, i);
        }
        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            for (String provider : i.providers()) {
                // Register all constructors as security providers may use
                // java.security.Provider.Service.newInstance(Object constructorParameter) to instantiate the service
                // through a different, than the nullary, constructor.
                addReflectiveClass(reflectiveClasses, Collections.emptySet(),
                        ReflectiveClassBuildItem.builder(provider).reason("Class registered as provider").build());
                // Register public provider() method for lookkup to avoid throwing a MissingReflectionRegistrationError at run time.
                // See ServiceLoader#loadProvider and ServiceLoader#findStaticProviderMethod.
                addReflectiveMethod(reflectiveClasses,
                        new ReflectiveMethodBuildItem("Class registered as provider", provider, "provider"));
            }
        }

        // Perform this as last step, since it augments the already added reflective classes
        for (ReflectiveClassConditionBuildItem i : reflectiveClassConditionBuildItems) {
            reflectiveClasses.computeIfPresent(i.getClassName(), (key, value) -> {
                value.typeReached = i.getTypeReached();
                return value;
            });
        }

        if (reflectiveClasses.isEmpty()) {
            return;
        }

        final JsonArrayBuilder reflectionArray = Json.array();
        for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {
            final JsonObjectBuilder json = Json.object().put("type", entry.getKey());
            final ReflectionInfo info = entry.getValue();
            final JsonArrayBuilder methodsArray = Json.array();
            if (info.typeReached != null) {
                json.put("condition", Json.object().put("typeReached", info.typeReached));
            }
            if (info.constructors) {
                json.put("allDeclaredConstructors", true);
            } else if (!info.ctorSet.isEmpty()) {
                extractToJsonArray(info.ctorSet, methodsArray);
            }
            if (info.publicConstructors) {
                json.put("allPublicConstructors", true);
            }
            if (info.methods) {
                json.put("allDeclaredMethods", true);
            } else {
                if (!info.methodSet.isEmpty()) {
                    extractToJsonArray(info.methodSet, methodsArray);
                }
            }
            if (info.publicMethods) {
                json.put("allPublicMethods", true);
            }
            if (!methodsArray.isEmpty()) {
                json.put("methods", methodsArray);
            }
            // reachability-metadata-schema-v1.2.0 delegates fields to DCE, i.e.
            // seemingly including *more* in config doesn't necessarily translate to a bloatier image.
            addAllDeclaredFields(info.fields, info.fieldSet, methodsArray, json);
            if (info.unsafeAllocated) {
                json.put("unsafeAllocated", true);
            }
            if (info.serialization) {
                json.put("serializable", true);
            }
            if (nativeConfig.includeReasonsInConfigFiles() && info.reasons != null
                    && !info.reasons.isEmpty()) {
                final JsonArrayBuilder reasonsArray = Json.array();
                reasonsArray.addAll(info.reasons);
                json.put("reason", reasonsArray);
            }
            reflectionArray.add(json);
        }

        final JsonObjectBuilder root = Json.object().put("reflection", reflectionArray);
        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            reflectConfig.produce(new GeneratedResourceBuildItem(
                    "META-INF/native-image/reflect/reachability-metadata.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void addAllDeclaredFields(boolean fields, Set<String> fieldSet, JsonArrayBuilder methodsArray,
            JsonObjectBuilder json) {
        if (!methodsArray.isEmpty()) {
            json.put("methods", methodsArray);
        }
        if (fields) {
            json.put("allDeclaredFields", true);
        } else if (!fieldSet.isEmpty()) {
            final JsonArrayBuilder fieldsArray = Json.array();
            for (String fieldName : fieldSet) {
                fieldsArray.add(Json.object().put("name", fieldName));
            }
            json.put("fields", fieldsArray);
        }
    }

    private static void extractToJsonArray(Set<ReflectiveMethodBuildItem> methodSet, JsonArrayBuilder methodsArray) {
        for (ReflectiveMethodBuildItem method : methodSet) {
            final JsonArrayBuilder paramsArray = Json.array();
            Collections.addAll(paramsArray, method.getParams());
            methodsArray.add(Json.object()
                    .put("name", method.getName())
                    .put("parameterTypes", paramsArray));
        }
    }

    private static void addReflectiveMethod(Map<String, ReflectionInfo> reflectiveClasses,
            ReflectiveMethodBuildItem methodInfo) {
        final ReflectionInfo existing = reflectiveClasses
                .computeIfAbsent(methodInfo.getDeclaringClass(),
                        k -> new ReflectionInfo());
        if ("<init>".equals(methodInfo.getName())) {
            existing.ctorSet.add(methodInfo);
        } else {
            existing.methodSet.add(methodInfo);
        }
        final String reason = methodInfo.getReason();
        if (reason != null) {
            if (existing.reasons == null) {
                existing.reasons = new HashSet<>();
            }
            existing.reasons.add(reason);
        }
    }

    private static void addReflectiveClass(Map<String, ReflectionInfo> reflectiveClasses,
            Set<String> forcedNonWeakClasses,
            ReflectiveClassBuildItem classBuildItem) {
        for (String cl : classBuildItem.getClassNames()) {
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                String typeReached = (!forcedNonWeakClasses.contains(cl) && classBuildItem.isWeak()) ? cl : null;
                reflectiveClasses.put(cl, new ReflectionInfo(classBuildItem, typeReached));
            } else {
                if (classBuildItem.isConstructors()) {
                    existing.constructors = true;
                }
                if (classBuildItem.isPublicConstructors()) {
                    existing.publicConstructors = true;
                }
                if (classBuildItem.isQueryConstructors()) {
                    existing.queryConstructors = true;
                }
                if (classBuildItem.isMethods()) {
                    existing.methods = true;
                }
                if (classBuildItem.isPublicMethods()) {
                    existing.publicMethods = true;
                }
                if (classBuildItem.isQueryMethods()) {
                    existing.queryMethods = true;
                }
                if (classBuildItem.isFields()) {
                    existing.fields = true;
                }
                if (classBuildItem.isClasses()) {
                    existing.classes = true;
                }
                if (classBuildItem.isSerialization()) {
                    existing.serialization = true;
                }
                if (classBuildItem.isUnsafeAllocated()) {
                    existing.unsafeAllocated = true;
                }
                if (classBuildItem.getReason() != null) {
                    if (existing.reasons == null) {
                        existing.reasons = new HashSet<>();
                    }
                    existing.reasons.add(classBuildItem.getReason());
                }
            }
        }
    }

    private static void addReflectiveField(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveFieldBuildItem fieldInfo) {
        final ReflectionInfo existing = reflectiveClasses.computeIfAbsent(fieldInfo.getDeclaringClass(),
                k -> new ReflectionInfo());
        existing.fieldSet.add(fieldInfo.getName());
        final String reason = fieldInfo.getReason();
        if (reason != null) {
            if (existing.reasons == null) {
                existing.reasons = new HashSet<>();
            }
            existing.reasons.add(reason);
        }
    }

    static final class ReflectionInfo {
        boolean constructors;
        boolean publicConstructors;
        boolean queryConstructors;
        boolean methods;
        boolean publicMethods;
        boolean queryMethods;
        boolean fields;
        boolean classes;
        boolean serialization;
        boolean unsafeAllocated;
        Set<String> reasons = null;
        String typeReached;
        final Set<String> fieldSet = new HashSet<>();
        final Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        final Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo() {
        }

        private ReflectionInfo(ReflectiveClassBuildItem classBuildItem, String typeReached) {
            this.methods = classBuildItem.isMethods();
            this.publicMethods = classBuildItem.isPublicMethods();
            this.queryMethods = classBuildItem.isQueryMethods();
            this.fields = classBuildItem.isFields();
            this.classes = classBuildItem.isClasses();
            this.typeReached = typeReached;
            this.constructors = classBuildItem.isConstructors();
            this.publicConstructors = classBuildItem.isPublicConstructors();
            this.serialization = classBuildItem.isSerialization();
            this.unsafeAllocated = classBuildItem.isUnsafeAllocated();
            if (classBuildItem.getReason() != null) {
                this.reasons = new HashSet<>();
                this.reasons.add(classBuildItem.getReason());
            }
        }
    }
}
