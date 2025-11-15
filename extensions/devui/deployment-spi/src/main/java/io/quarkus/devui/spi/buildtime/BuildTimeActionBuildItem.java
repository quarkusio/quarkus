package io.quarkus.devui.spi.buildtime;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.devui.spi.AbstractDevUIBuildItem;
import io.quarkus.devui.spi.buildtime.jsonrpc.AbstractJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.DeploymentJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.RecordedJsonRpcMethod;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Usage;

/**
 * Holds any Build time actions for Dev UI the extension has
 */
public final class BuildTimeActionBuildItem extends AbstractDevUIBuildItem {

    private final List<DeploymentJsonRpcMethod> deploymentActions = new ArrayList<>();
    private final List<DeploymentJsonRpcMethod> deploymentSubscriptions = new ArrayList<>();

    private final List<RecordedJsonRpcMethod> recordedActions = new ArrayList<>();
    private final List<RecordedJsonRpcMethod> recordedSubscriptions = new ArrayList<>();

    public BuildTimeActionBuildItem() {
        super();
    }

    public BuildTimeActionBuildItem(String customIdentifier) {
        super(customIdentifier);
    }

    public List<DeploymentJsonRpcMethod> getDeploymentActions() {
        return this.deploymentActions;
    }

    public List<RecordedJsonRpcMethod> getRecordedActions() {
        return this.recordedActions;
    }

    public List<DeploymentJsonRpcMethod> getDeploymentSubscriptions() {
        return deploymentSubscriptions;
    }

    public List<RecordedJsonRpcMethod> getRecordedSubscriptions() {
        return recordedSubscriptions;
    }

    public ActionBuilder actionBuilder() {
        return new ActionBuilder();
    }

    public SubscriptionBuilder subscriptionBuilder() {
        return new SubscriptionBuilder();
    }

    @Deprecated
    public <T> void addAction(String methodName,
            Function<Map<String, String>, T> action) {
        this.addAction(new DeploymentJsonRpcMethod(methodName, null, Usage.onlyDevUI(), true, action));
    }

    @Deprecated
    public <T> void addAssistantAction(String methodName,
            BiFunction<Object, Map<String, String>, T> action) {
        this.addAction(new DeploymentJsonRpcMethod(methodName, null, Usage.onlyDevUI(), true, action));
    }

    @Deprecated
    public <T> void addAction(String methodName,
            RuntimeValue runtimeValue) {
        this.addAction(new RecordedJsonRpcMethod(methodName, null, Usage.onlyDevUI(), true, runtimeValue));
    }

    @Deprecated
    public <T> void addSubscription(String methodName,
            Function<Map<String, String>, T> action) {
        this.addSubscription(new DeploymentJsonRpcMethod(methodName, null, Usage.onlyDevUI(), true, action));
    }

    @Deprecated
    public <T> void addSubscription(String methodName,
            RuntimeValue runtimeValue) {
        this.addSubscription(new RecordedJsonRpcMethod(methodName, null, Usage.onlyDevUI(), true, runtimeValue));
    }

    private BuildTimeActionBuildItem addAction(DeploymentJsonRpcMethod deploymentJsonRpcMethod) {
        this.deploymentActions.add(deploymentJsonRpcMethod);
        return this;
    }

    private BuildTimeActionBuildItem addAction(RecordedJsonRpcMethod recordedJsonRpcMethod) {
        this.recordedActions.add(recordedJsonRpcMethod);
        return this;
    }

    private BuildTimeActionBuildItem addSubscription(DeploymentJsonRpcMethod deploymentJsonRpcMethod) {
        this.deploymentSubscriptions.add(deploymentJsonRpcMethod);
        return this;
    }

    private BuildTimeActionBuildItem addSubscription(RecordedJsonRpcMethod recordedJsonRpcMethod) {
        this.recordedSubscriptions.add(recordedJsonRpcMethod);
        return this;
    }

    public final class ActionBuilder {
        private String methodName;
        private String description;
        private Map<String, AbstractJsonRpcMethod.Parameter> parameters = new LinkedHashMap<>();
        private EnumSet<Usage> usage;
        private Function<Map<String, String>, ?> function;
        private BiFunction<Object, Map<String, String>, ?> assistantFunction;
        private RuntimeValue<?> runtimeValue;
        private boolean mcpEnabledByDefault = false;

        public ActionBuilder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public ActionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ActionBuilder parameter(String name, String description) {
            return parameter(name, String.class, description);
        }

        public ActionBuilder parameter(String name, String description, boolean required) {
            return parameter(name, String.class, description, required);
        }

        public ActionBuilder parameter(String name, Class<?> type, String description) {
            this.parameters.put(name, new AbstractJsonRpcMethod.Parameter(type, description, true));
            return this;
        }

        public ActionBuilder parameter(String name, Class<?> type, String description, boolean required) {
            this.parameters.put(name, new AbstractJsonRpcMethod.Parameter(type, description, required));
            return this;
        }

        public ActionBuilder usage(EnumSet<Usage> usage) {
            this.usage = usage;
            return this;
        }

        public ActionBuilder enableMcpFuctionByDefault() {
            this.mcpEnabledByDefault = true;
            return this;
        }

        public <T> ActionBuilder function(Function<Map<String, String>, T> function) {
            if (this.runtimeValue != null || this.assistantFunction != null)
                throw new IllegalStateException("Only one of runtimeValue, function or assistantFunction is allowed");
            this.function = function;
            return this;
        }

        public <T> ActionBuilder assistantFunction(BiFunction<Object, Map<String, String>, ?> assistantFunction) {
            if (this.function != null || this.runtimeValue != null)
                throw new IllegalStateException("Only one of runtimeValue, function or assistantFunction is allowed");
            this.assistantFunction = assistantFunction;
            return this;
        }

        public ActionBuilder runtime(RuntimeValue<?> runtimeValue) {
            if (this.function != null || this.assistantFunction != null)
                throw new IllegalStateException("Only one of runtimeValue, function or assistantFunction is allowed");
            this.runtimeValue = runtimeValue;
            return this;
        }

        public BuildTimeActionBuildItem build() {
            if (methodName == null || methodName.isBlank())
                throw new IllegalStateException("methodName must be provided");
            if (parameters.isEmpty())
                parameters = null;
            if (function != null) {
                return addAction(
                        new DeploymentJsonRpcMethod(methodName, description, parameters, autoUsage(usage, description),
                                mcpEnabledByDefault,
                                function));
            } else if (runtimeValue != null) {
                return addAction(
                        new RecordedJsonRpcMethod(methodName, description, autoUsage(usage, description),
                                mcpEnabledByDefault,
                                runtimeValue));
            } else if (assistantFunction != null) {
                return addAction(
                        new DeploymentJsonRpcMethod(methodName, description, parameters, autoUsage(usage, description),
                                mcpEnabledByDefault,
                                assistantFunction));
            } else {
                throw new IllegalStateException("Either function, assistantFunction or runtimeValue must be provided");
            }
        }
    }

    public final class SubscriptionBuilder {
        private String methodName;
        private String description;
        private Map<String, AbstractJsonRpcMethod.Parameter> parameters = new LinkedHashMap<>();;
        private EnumSet<Usage> usage;
        private boolean mcpEnabledByDefault = false;
        private Function<Map<String, String>, ?> function;
        private RuntimeValue<?> runtimeValue;

        public SubscriptionBuilder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public SubscriptionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SubscriptionBuilder parameter(String name, String description) {
            return parameter(name, String.class, description);
        }

        public SubscriptionBuilder parameter(String name, String description, boolean required) {
            return parameter(name, String.class, description, required);
        }

        public SubscriptionBuilder parameter(String name, Class<?> type, String description) {
            this.parameters.put(name, new AbstractJsonRpcMethod.Parameter(type, description, true));
            return this;
        }

        public SubscriptionBuilder parameter(String name, Class<?> type, String description, boolean required) {
            this.parameters.put(name, new AbstractJsonRpcMethod.Parameter(type, description, required));
            return this;
        }

        public SubscriptionBuilder usage(EnumSet<Usage> usage) {
            this.usage = usage;
            return this;
        }

        public SubscriptionBuilder enableMcpFuctionByDefault() {
            this.mcpEnabledByDefault = true;
            return this;
        }

        public <T> SubscriptionBuilder function(Function<Map<String, String>, T> function) {
            this.function = function;
            return this;
        }

        public SubscriptionBuilder runtime(RuntimeValue<?> runtimeValue) {
            this.runtimeValue = runtimeValue;
            return this;
        }

        public void build() {
            if (methodName == null || methodName.isBlank())
                throw new IllegalStateException("methodName must be provided");
            if (parameters.isEmpty())
                parameters = null;
            if (function != null) {
                addSubscription(new DeploymentJsonRpcMethod(methodName, description, parameters, autoUsage(usage, description),
                        mcpEnabledByDefault,
                        function));
            } else if (runtimeValue != null) {
                addSubscription(
                        new RecordedJsonRpcMethod(methodName, description, autoUsage(usage, description), mcpEnabledByDefault,
                                runtimeValue));
            } else {
                throw new IllegalStateException("Either function or runtimeValue must be provided");
            }
        }
    }

    private EnumSet<Usage> autoUsage(EnumSet<Usage> usage, String description) {
        if (usage == null && description == null) {
            usage = Usage.onlyDevUI();
        } else if (usage == null) {
            usage = Usage.devUIandDevMCP();
        }
        return usage;
    }
}
