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
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageReflectConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateReflectConfig(BuildProducer<GeneratedResourceBuildItem> reflectConfig,
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
            addReflectiveClass(reflectiveClasses, forcedNonWeakClasses, i.isConstructors(), i.isQueryConstructors(),
                    i.isMethods(), i.isQueryMethods(), i.isFields(),
                    i.isWeak(), i.isSerialization(), i.isUnsafeAllocated(), i.getClassNames().toArray(new String[0]));
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
                addReflectiveMethod(reflectiveClasses, new ReflectiveMethodBuildItem(provider, "<init>", new String[0]));
                // Register public provider() method for lookkup to avoid throwing a MissingReflectionRegistrationError at run time.
                // See ServiceLoader#loadProvider and ServiceLoader#findStaticProviderMethod.
                addReflectiveMethod(reflectiveClasses,
                        new ReflectiveMethodBuildItem(true, provider, "provider", new String[0]));
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
            if (info.unsafeAllocated) {
                json.put("unsafeAllocated", true);
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
    }

    public void addReflectiveClass(Map<String, ReflectionInfo> reflectiveClasses, Set<String> forcedNonWeakClasses,
            boolean constructors, boolean queryConstructors, boolean method,
            boolean queryMethods, boolean fields, boolean weak, boolean serialization, boolean unsafeAllocated,
            String... className) {
        for (String cl : className) {
            ReflectionInfo existing = reflectiveClasses.get(cl);
            if (existing == null) {
                String typeReachable = (!forcedNonWeakClasses.contains(cl) && weak) ? cl : null;
                reflectiveClasses.put(cl, new ReflectionInfo(constructors, queryConstructors, method, queryMethods, fields,
                        typeReachable, serialization, unsafeAllocated));
            } else {
                if (constructors) {
                    existing.constructors = true;
                }
                if (queryConstructors) {
                    existing.queryConstructors = true;
                }
                if (method) {
                    existing.methods = true;
                }
                if (queryMethods) {
                    existing.queryMethods = true;
                }
                if (fields) {
                    existing.fields = true;
                }
                if (serialization) {
                    existing.serialization = true;
                }
                if (unsafeAllocated) {
                    existing.unsafeAllocated = true;
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
    }

    static final class ReflectionInfo {
        boolean constructors;
        boolean queryConstructors;
        boolean methods;
        boolean queryMethods;
        boolean fields;
        boolean serialization;
        boolean unsafeAllocated;
        String typeReachable;
        Set<String> fieldSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> methodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> queriedMethodSet = new HashSet<>();
        Set<ReflectiveMethodBuildItem> ctorSet = new HashSet<>();

        private ReflectionInfo() {
            this(false, false, false, false, false, null, false, false);
        }

        private ReflectionInfo(boolean constructors, boolean queryConstructors, boolean methods, boolean queryMethods,
                boolean fields, String typeReachable,
                boolean serialization, boolean unsafeAllocated) {
            this.methods = methods;
            this.queryMethods = queryMethods;
            this.fields = fields;
            this.typeReachable = typeReachable;
            this.constructors = constructors;
            this.queryConstructors = queryConstructors;
            this.serialization = serialization;
            this.unsafeAllocated = unsafeAllocated;
        }
    }

}
