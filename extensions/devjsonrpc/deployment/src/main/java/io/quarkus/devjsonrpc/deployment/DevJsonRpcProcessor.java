package io.quarkus.devjsonrpc.deployment;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devjsonrpc.runtime.DevJsonRpcRecorder;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devjsonrpc.spi.jsonrpc.AbstractJsonRpcMethod;
import io.quarkus.devjsonrpc.spi.jsonrpc.DeploymentJsonRpcMethod;
import io.quarkus.devjsonrpc.spi.jsonrpc.RecordedJsonRpcMethod;
import io.quarkus.devjsonrpc.spi.jsonrpc.RuntimeJsonRpcMethod;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;

/**
 * Build steps for the JSON-RPC engine.
 * This discovers all jsonrpc methods and makes them available in the jsonRPC Router.
 */
public class DevJsonRpcProcessor {

    private static final String CONSTRUCTOR = "<init>";
    private static final String UNDERSCORE = "_";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems) {
        // Make the brean providers unremovable
        for (JsonRPCProvidersBuildItem jsonRPCProvidersBuildItem : jsonRPCProvidersBuildItems) {
            additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(jsonRPCProvidersBuildItem.getJsonRPCMethodProviderClass())
                    .setUnremovable()
                    .build());
        }

        // Make JsonRpcRouter an unremovable bean
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonRpcRouter.class)
                .setUnremovable()
                .build());
    }

    /**
     * This goes through all jsonRPC methods and discovers the methods using Jandex
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void findAllJsonRPCMethods(BuildProducer<JsonRPCRuntimeMethodsBuildItem> jsonRPCMethodsProvider,
            LaunchModeBuildItem launchModeBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JsonRPCProvidersBuildItem> jsonRPCProvidersBuildItems,
            Optional<DeploymentMethodBuildItem> deploymentMethodBuildItem) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        IndexView index = combinedIndexBuildItem.getIndex();

        Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap = new HashMap<>();// All methods to execute against the runtime classpath
        Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap = new HashMap<>();// All subscriptions to execute against the runtime classpath

        DotName descriptionAnnotation = DotName.createSimple(JsonRpcDescription.class);
        DotName devMCPEnableByDefaultAnnotation = DotName.createSimple(DevMCPEnableByDefault.class);

        // Let's use the Jandex index to find all methods
        for (JsonRPCProvidersBuildItem jsonRPCProvidersBuildItem : jsonRPCProvidersBuildItems) {

            Class clazz = jsonRPCProvidersBuildItem.getJsonRPCMethodProviderClass();
            String extension = jsonRPCProvidersBuildItem.getExtensionPathName(curateOutcomeBuildItem);

            ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazz.getName()));
            if (classInfo != null) {// skip if not found
                for (MethodInfo method : classInfo.methods()) {
                    // Ignore constructor, Only allow public methods, Only allow method with response
                    if (!method.name().equals(CONSTRUCTOR) && Modifier.isPublic(method.flags())
                            && method.returnType().kind() != Type.Kind.VOID) {

                        String methodName = extension + UNDERSCORE + method.name();

                        Map<String, AbstractJsonRpcMethod.Parameter> parameters = new LinkedHashMap<>(); // Keep the order
                        for (int i = 0; i < method.parametersCount(); i++) {
                            String description = null;
                            boolean required = true;
                            Type parameterType = method.parameterType(i);
                            if (DotNames.OPTIONAL.equals(parameterType.name())) {
                                required = false;
                                parameterType = parameterType.asParameterizedType().arguments().get(0);
                            }
                            AnnotationInstance jsonRpcDescriptionAnnotation = method.parameters().get(i)
                                    .annotation(descriptionAnnotation);
                            if (jsonRpcDescriptionAnnotation != null) {
                                AnnotationValue descriptionValue = jsonRpcDescriptionAnnotation.value();
                                if (descriptionValue != null && !descriptionValue.asString().isBlank()) {
                                    description = descriptionValue.asString();
                                }
                            }
                            Class<?> parameterClass = toClass(parameterType);
                            String parameterName = method.parameterName(i);
                            parameters.put(parameterName,
                                    new AbstractJsonRpcMethod.Parameter(parameterClass, description, required));
                        }

                        // Look for @JsonRpcUsage annotation
                        EnumSet<Usage> usage = EnumSet.noneOf(Usage.class);
                        AnnotationInstance jsonRpcUsageAnnotation = method.annotation(DotName.createSimple(JsonRpcUsage.class));
                        if (jsonRpcUsageAnnotation != null) {
                            String[] usageArray = jsonRpcUsageAnnotation.value().asEnumArray();

                            for (String usageStr : usageArray) {
                                usage.add(Usage.valueOf(usageStr));
                            }
                        }

                        // Look for @JsonRpcDescription annotation
                        String description = null;
                        boolean mcpEnabledByDefault = false;
                        AnnotationInstance jsonRpcDescriptionAnnotation = method
                                .annotation(descriptionAnnotation);
                        if (jsonRpcDescriptionAnnotation != null) {
                            AnnotationValue descriptionValue = jsonRpcDescriptionAnnotation.value();
                            if (descriptionValue != null && !descriptionValue.asString().isBlank()) {
                                description = descriptionValue.asString();
                                usage = Usage.devUIandDevMCP();
                            }

                            AnnotationInstance devMCPEnableByDefaultAnnotationInstance = method
                                    .annotation(devMCPEnableByDefaultAnnotation);
                            if (devMCPEnableByDefaultAnnotationInstance != null) {
                                mcpEnabledByDefault = true;
                            }
                        } else {
                            usage = Usage.onlyDevUI();
                        }

                        RuntimeJsonRpcMethod runtimeJsonRpcMethod = new RuntimeJsonRpcMethod(methodName, description,
                                parameters,
                                usage,
                                mcpEnabledByDefault,
                                clazz,
                                method.hasAnnotation(Blocking.class), method.hasAnnotation(NonBlocking.class));

                        // Create list of available methods for the Javascript side.
                        if (method.returnType().name().equals(DotName.createSimple(Multi.class.getName()))) {
                            runtimeSubscriptionsMap.put(methodName, runtimeJsonRpcMethod);
                        } else {
                            runtimeMethodsMap.put(methodName, runtimeJsonRpcMethod);
                        }

                    }
                }
            }
        }

        jsonRPCMethodsProvider.produce(new JsonRPCRuntimeMethodsBuildItem(runtimeMethodsMap, runtimeSubscriptionsMap));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void createJsonRpcRouter(DevJsonRpcRecorder recorder,
            BeanContainerBuildItem beanContainer,
            Optional<JsonRPCRuntimeMethodsBuildItem> jsonRPCMethodsBuildItem,
            Optional<DeploymentMethodBuildItem> deploymentMethodBuildItem) {

        Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap = jsonRPCMethodsBuildItem
                .map(JsonRPCRuntimeMethodsBuildItem::getRuntimeMethodsMap).orElse(Collections.emptyMap());
        Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap = jsonRPCMethodsBuildItem
                .map(JsonRPCRuntimeMethodsBuildItem::getRuntimeSubscriptionsMap).orElse(Collections.emptyMap());

        if (DevConsoleManager.getGlobal(DevJsonRpcRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY) == null) {
            DevConsoleManager.setGlobal(DevJsonRpcRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY,
                    JsonMapper.Factory.deploymentLinker().createLinkData(new DevJsonRpcDatabindCodec.Factory()));
        }

        recorder.createJsonRpcRouter(beanContainer.getValue(),
                runtimeToJsonRpcMethods(runtimeMethodsMap),
                runtimeToJsonRpcMethods(runtimeSubscriptionsMap),
                deploymentToJsonRpcMethods(
                        deploymentMethodBuildItem.map(DeploymentMethodBuildItem::getMethods).orElse(Collections.emptyMap())),
                deploymentToJsonRpcMethods(deploymentMethodBuildItem.map(DeploymentMethodBuildItem::getSubscriptions)
                        .orElse(Collections.emptyMap())),
                recordedToJsonRpcMethods(deploymentMethodBuildItem.map(DeploymentMethodBuildItem::getRecordedMethods)
                        .orElse(Collections.emptyMap())),
                recordedToJsonRpcMethods(deploymentMethodBuildItem.map(DeploymentMethodBuildItem::getRecordedSubscriptions)
                        .orElse(Collections.emptyMap())));
    }

    private Map<String, JsonRpcMethod> runtimeToJsonRpcMethods(Map<String, RuntimeJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::runtimeToJsonRpcMethod);
    }

    private Map<String, JsonRpcMethod> deploymentToJsonRpcMethods(Map<String, DeploymentJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::toJsonRpcMethod);
    }

    private Map<String, JsonRpcMethod> recordedToJsonRpcMethods(Map<String, RecordedJsonRpcMethod> m) {
        return mapToJsonRpcMethods(m, this::recordedToJsonRpcMethod);
    }

    private <T extends AbstractJsonRpcMethod> Map<String, JsonRpcMethod> mapToJsonRpcMethods(
            Map<String, T> input,
            Function<T, JsonRpcMethod> converter) {

        return input.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> converter.apply(e.getValue())));
    }

    private JsonRpcMethod runtimeToJsonRpcMethod(RuntimeJsonRpcMethod i) {
        JsonRpcMethod o = toJsonRpcMethod(i);

        o.setBean(i.getBean());
        o.setIsExplicitlyBlocking(i.isExplicitlyBlocking());
        o.setIsExplicitlyNonBlocking(i.isExplicitlyNonBlocking());

        return o;
    }

    private JsonRpcMethod recordedToJsonRpcMethod(RecordedJsonRpcMethod i) {
        JsonRpcMethod o = toJsonRpcMethod(i);
        o.setRuntimeValue(i.getRuntimeValue());
        return o;
    }

    private JsonRpcMethod toJsonRpcMethod(AbstractJsonRpcMethod i) {
        JsonRpcMethod o = new JsonRpcMethod();

        o.setMethodName(i.getMethodName());
        o.setDescription(i.getDescription());
        o.setUsage(List.copyOf(i.getUsage()));
        o.setMcpEnabledByDefault(i.isMcpEnabledByDefault());
        if (i.hasParameters()) {
            for (Map.Entry<String, AbstractJsonRpcMethod.Parameter> ip : i.getParameters().entrySet()) {
                o.addParameter(ip.getKey(), ip.getValue().getType(), ip.getValue().getDescription(),
                        ip.getValue().isRequired());
            }
        }

        return o;
    }

    private Class toClass(Type type) {
        switch (type.kind()) {
            case PRIMITIVE:
                return switch (type.asPrimitiveType().primitive()) {
                    case BOOLEAN -> boolean.class;
                    case BYTE -> byte.class;
                    case CHAR -> char.class;
                    case DOUBLE -> double.class;
                    case FLOAT -> float.class;
                    case INT -> int.class;
                    case LONG -> long.class;
                    case SHORT -> short.class;
                };
            default:
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(type.name().toString());
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
        }
    }
}
