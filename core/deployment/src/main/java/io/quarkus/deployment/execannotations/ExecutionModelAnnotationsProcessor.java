package io.quarkus.deployment.execannotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.common.annotation.SuppressForbidden;

public class ExecutionModelAnnotationsProcessor {
    private static final Logger log = Logger.getLogger(ExecutionModelAnnotationsProcessor.class);

    private static final DotName BLOCKING = DotName.createSimple(Blocking.class);
    private static final DotName NON_BLOCKING = DotName.createSimple(NonBlocking.class);
    private static final DotName RUN_ON_VIRTUAL_THREAD = DotName.createSimple(RunOnVirtualThread.class);

    @BuildStep
    @Produce(GeneratedClassBuildItem.class) // only to make sure this build step is executed
    void check(ExecutionModelAnnotationsConfig config, CombinedIndexBuildItem index,
            List<ExecutionModelAnnotationsAllowedBuildItem> predicates) {

        if (config.detectionMode() == ExecutionModelAnnotationsConfig.Mode.DISABLED) {
            return;
        }

        StringBuilder message = new StringBuilder("\n");
        doCheck(message, index.getIndex(), predicates, BLOCKING);
        doCheck(message, index.getIndex(), predicates, NON_BLOCKING);
        doCheck(message, index.getIndex(), predicates, RUN_ON_VIRTUAL_THREAD);

        if (message.length() > 1) {
            message.append("The @Blocking, @NonBlocking and @RunOnVirtualThread annotations may only be used "
                    + "on \"entrypoint\" methods (methods invoked by various frameworks in Quarkus)\n");
            message.append("Using the @Blocking, @NonBlocking and @RunOnVirtualThread annotations on methods "
                    + "that can only be invoked by application code is invalid");
            if (config.detectionMode() == ExecutionModelAnnotationsConfig.Mode.WARN) {
                log.warn(message);
            } else {
                throw new IllegalStateException(message.toString());
            }
        }
    }

    private void doCheck(StringBuilder message, IndexView index,
            List<ExecutionModelAnnotationsAllowedBuildItem> predicates, DotName annotationName) {

        List<String> badMethods = new ArrayList<>();
        for (AnnotationInstance annotation : index.getAnnotations(annotationName)) {
            // these annotations may be put on classes too, but we'll ignore that for now
            if (annotation.target() != null && annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo method = annotation.target().asMethod();
                boolean allowed = false;
                for (ExecutionModelAnnotationsAllowedBuildItem predicate : predicates) {
                    if (predicate.matches(method)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    badMethods.add(methodToString(method));
                }
            }
        }

        if (!badMethods.isEmpty()) {
            message.append("Wrong usage(s) of @").append(annotationName.withoutPackagePrefix()).append(" found:\n");
            for (String method : badMethods) {
                message.append("\t- ").append(method).append("\n");
            }
        }
    }

    /**
     * @deprecated this method will be removed in Quarkus 3.24, which gives extensions 2 releases
     *             to start producing {@code JsonRPCProvidersBuildItem} always, not just in dev mode
     */
    @Deprecated(since = "3.22", forRemoval = true)
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem devuiJsonRpcServices() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // gross hack to allow methods declared in Dev UI JSON RPC service classes,
                // as the proper way (consuming `JsonRPCProvidersBuildItem`) only works in dev mode
                String clazz = method.declaringClass().name().toString().toLowerCase(Locale.ROOT);
                return clazz.startsWith("io.quarkus.")
                        || clazz.startsWith("io.quarkiverse.")
                        || clazz.endsWith("jsonrpcservice");
            }
        });
    }

    @SuppressForbidden(reason = "Using Type.toString() to build an informative message")
    private String methodToString(MethodInfo method) {
        StringBuilder result = new StringBuilder();
        result.append(method.declaringClass().name()).append('.').append(method.name());
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (Type parameter : method.parameterTypes()) {
            joiner.add(parameter.toString());
        }
        result.append(joiner);
        return result.toString();
    }
}
