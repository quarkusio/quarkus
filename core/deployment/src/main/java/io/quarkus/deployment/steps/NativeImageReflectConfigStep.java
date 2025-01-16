package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.Json;
import io.quarkus.builder.Json.JsonArrayBuilder;
import io.quarkus.builder.Json.JsonObjectBuilder;
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
        for (ForceNonWeakReflectiveClassBuildItem nonWeakReflectiveClassBuildItem : nonWeakReflectiveClassBuildItems) {
            forcedNonWeakClasses.add(nonWeakReflectiveClassBuildItem.getClassName());
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
                // Register the nullary constructor
                addReflectiveMethod(reflectiveClasses,
                        new ReflectiveMethodBuildItem("Class registered as provider", provider, "<init>", new String[0]));
                // Register public provider() method for lookkup to avoid throwing a MissingReflectionRegistrationError at run time.
                // See ServiceLoader#loadProvider and ServiceLoader#findStaticProviderMethod.
                addReflectiveMethod(reflectiveClasses,
                        new ReflectiveMethodBuildItem("Class registered as provider", true, provider, "provider",
                                new String[0]));
            }
        }

        // Perform this as last step, since it augments the already added reflective classes
        for (ReflectiveClassConditionBuildItem i : reflectiveClassConditionBuildItems) {
            reflectiveClasses.computeIfPresent(i.getClassName(), (key, value) -> {
                value.typeReachable = i.getTypeReachable();
                return value;
            });
        }

        JsonArrayBuilder root = Json.array();
        for (Map.Entry<String, ReflectionInfo> entry : reflectiveClasses.entrySet()) {
            JsonObjectBuilder json = Json.object();

            json.put("name", entry.getKey());

            ReflectionInfo info = entry.getValue();
            JsonArrayBuilder methodsArray = Json.array();
            JsonArrayBuilder queriedMethodsArray = Json.array();
            if (info.typeReachable != null) {
                json.put("condition", Json.object().put("typeReachable", info.typeReachable));
            }
            if (info.constructors) {
                json.put("allDeclaredConstructors", true);
            } else {
                if (info.queryConstructors) {
                    json.put("queryAllDeclaredConstructors", true);
                }
                if (!info.ctorSet.isEmpty()) {
                    extractToJsonArray(info.ctorSet, methodsArray);
                }
            }
            if (info.publicConstructors) {
                json.put("allPublicConstructors", true);
            }
            if (info.methods) {
                json.put("allDeclaredMethods", true);
            } else {
                if (info.queryMethods) {
                    json.put("queryAllDeclaredMethods", true);
                }
                if (!info.methodSet.isEmpty()) {
                    extractToJsonArray(info.methodSet, methodsArray);
                }
                if (!info.queriedMethodSet.isEmpty()) {
                    extractToJsonArray(info.queriedMethodSet, queriedMethodsArray);
                }
            }
            if (!methodsArray.isEmpty()) {
                json.put("methods", methodsArray);
            }
            if (!queriedMethodsArray.isEmpty()) {
                json.put("queriedMethods", queriedMethodsArray);
            }

            if (info.fields) {
                json.put("allDeclaredFields", true);
            } else if (!info.fieldSet.isEmpty()) {
                JsonArrayBuilder fieldsArray = Json.array();
                for (String fieldName : info.fieldSet) {
                    fieldsArray.add(Json.object().put("name", fieldName));
                }
                json.put("fields", fieldsArray);
            }
            if (info.classes) {
                json.put("allDeclaredClasses", true);
            }
            if (info.unsafeAllocated) {
                json.put("unsafeAllocated", true);
            }
            if (nativeConfig.includeReasonsInConfigFiles() && info.reasons != null) {
                JsonArrayBuilder reasonsArray = Json.array();
                for (String reason : info.reasons) {
                    reasonsArray.add(reason);
                }
                json.put("reasons", reasonsArray);
            }

            root.add(json);
        }

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            reflectConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/reflect-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractToJsonArray(Set<ReflectiveMethodBuildItem> methodSet, JsonArrayBuilder methodsArray) {
        for (ReflectiveMethodBuildItem method : methodSet) {
            JsonObjectBuilder methodObject = Json.object();
            methodObject.put("name", method.getName());
            JsonArrayBuilder paramsArray = Json.array();
            for (int i = 0; i < method.getParams().length; ++i) {
                paramsArray.add(method.getParams()[i]);
            }
            methodObject.put("parameterTypes", paramsArray);
            methodsArray.add(methodObject);
        }
    }

    public void addReflectiveMethod(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveMethodBuildItem methodInfo) {
        String cl = methodInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo());
        }
        if (methodInfo.getName().equals("<init>")) {
            existing.ctorSet.add(methodInfo);
        } else {
            if (methodInfo.isQueryOnly()) {
                existing.queriedMethodSet.add(methodInfo);
            } else {
                existing.methodSet.add(methodInfo);
            }
        }
        String reason = methodInfo.getReason();
        if (reason != null) {
            if (existing.reasons == null) {
                existing.reasons = new HashSet<>();
            }
            existing.reasons.add(reason);
        }
    }

    public void addReflectiveClass(Map<String, ReflectionInfo> reflectiveClasses, Set<String> forcedNonWeakClasses,
            ReflectiveClassBuildItem classBuildItem) {
        for (String cl : classBuildItem.getClassNames()) {
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                String typeReachable = (!forcedNonWeakClasses.contains(cl) && classBuildItem.isWeak()) ? cl : null;
                reflectiveClasses.put(cl, new ReflectionInfo(classBuildItem, typeReachable));
            } else {
                if (classBuildItem.isConstructors()) {
                    existing.constructors = true;
                }
                if (classBuildItem.isQueryConstructors()) {
                    existing.queryConstructors = true;
                }
                if (classBuildItem.isMethods()) {
                    existing.methods = true;
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

    public void addReflectiveField(Map<String, ReflectionInfo> reflectiveClasses, ReflectiveFieldBuildItem fieldInfo) {
        String cl = fieldInfo.getDeclaringClass();
        ReflectionInfo existing = reflectiveClasses.get(cl);
        if (existing == null) {
            reflectiveClasses.put(cl, existing = new ReflectionInfo());
        }
        existing.fieldSet.add(fieldInfo.getName());
        String reason = fieldInfo.getReason();
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
        boolean queryMethods;
        boolean fields;
        boolean classes;
        boolean serialization;
        boolean unsafeAllocated;
        Set<String> reasons = null;
        String typeReachable;
        Set<String> fieldSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> queriedMethodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo() {
        }

        private ReflectionInfo(ReflectiveClassBuildItem classBuildItem, String typeReachable) {
            this.methods = classBuildItem.isMethods();
            this.queryMethods = classBuildItem.isQueryMethods();
            this.fields = classBuildItem.isFields();
            this.classes = classBuildItem.isClasses();
            this.typeReachable = typeReachable;
            this.constructors = classBuildItem.isConstructors();
            this.publicConstructors = classBuildItem.isPublicConstructors();
            this.queryConstructors = classBuildItem.isQueryConstructors();
            this.serialization = classBuildItem.isSerialization();
            this.unsafeAllocated = classBuildItem.isUnsafeAllocated();
            if (classBuildItem.getReason() != null) {
                reasons = new HashSet<>();
                reasons.add(classBuildItem.getReason());
            }
        }
    }

}
