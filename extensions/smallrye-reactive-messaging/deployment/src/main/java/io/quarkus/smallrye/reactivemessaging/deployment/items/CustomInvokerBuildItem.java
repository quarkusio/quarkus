package io.quarkus.smallrye.reactivemessaging.deployment.items;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;

import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.reactive.messaging.MediatorConfiguration;
import io.smallrye.reactive.messaging.Shape;
import io.smallrye.reactive.messaging.annotations.Merge;

/**
 * Provides a pre-generated invoker class for a specific mediator method.
 * When present, the core messaging processor skips invoker generation for the matching method
 * and applies the specified mediator configuration overrides.
 * <p>
 * All override fields are nullable — when {@code null}, the default configuration from
 * the mediator annotation processing is preserved.
 */
public final class CustomInvokerBuildItem extends MultiBuildItem {

    private final String declaringClassName;
    private final String methodName;
    private final String invokerClassName;
    private final List<String> syntheticParameterTypes;
    // Overrides for MediatorConfiguration
    private final Shape shape;
    private final MediatorConfiguration.Production production;
    private final MediatorConfiguration.Consumption consumption;
    private final Acknowledgment.Strategy acknowledgment;
    private final Merge.Mode merge;
    private final Boolean blocking;

    private CustomInvokerBuildItem(Builder builder) {
        this.declaringClassName = builder.declaringClassName;
        this.methodName = builder.methodName;
        this.invokerClassName = builder.invokerClassName;
        this.shape = builder.shape;
        this.production = builder.production;
        this.consumption = builder.consumption;
        this.acknowledgment = builder.acknowledgment;
        this.merge = builder.merge;
        this.blocking = builder.blocking;
        this.syntheticParameterTypes = builder.syntheticParameterTypes != null
                ? builder.syntheticParameterTypes
                : List.of();
    }

    public static Builder builder(String declaringClassName, String methodName, String invokerClassName) {
        return new Builder(declaringClassName, methodName, invokerClassName);
    }

    public String getMethodId() {
        return declaringClassName + "#" + methodName;
    }

    public String getInvokerClassName() {
        return invokerClassName;
    }

    public Shape getShape() {
        return shape;
    }

    public MediatorConfiguration.Production getProduction() {
        return production;
    }

    public MediatorConfiguration.Consumption getConsumption() {
        return consumption;
    }

    public Acknowledgment.Strategy getAcknowledgment() {
        return acknowledgment;
    }

    public Merge.Mode getMerge() {
        return merge;
    }

    public Boolean getBlocking() {
        return blocking;
    }

    public List<String> getSyntheticParameterTypes() {
        return syntheticParameterTypes;
    }

    public static class Builder {
        private final String declaringClassName;
        private final String methodName;
        private final String invokerClassName;
        private Shape shape;
        private MediatorConfiguration.Production production;
        private MediatorConfiguration.Consumption consumption;
        private Acknowledgment.Strategy acknowledgment;
        private Merge.Mode merge;
        private Boolean blocking;
        private List<String> syntheticParameterTypes;

        private Builder(String declaringClassName, String methodName, String invokerClassName) {
            this.declaringClassName = declaringClassName;
            this.methodName = methodName;
            this.invokerClassName = invokerClassName;
        }

        public Builder shape(Shape shape) {
            this.shape = shape;
            return this;
        }

        public Builder production(MediatorConfiguration.Production production) {
            this.production = production;
            return this;
        }

        public Builder consumption(MediatorConfiguration.Consumption consumption) {
            this.consumption = consumption;
            return this;
        }

        public Builder acknowledgment(Acknowledgment.Strategy acknowledgment) {
            this.acknowledgment = acknowledgment;
            return this;
        }

        public Builder merge(Merge.Mode merge) {
            this.merge = merge;
            return this;
        }

        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public Builder syntheticParameterTypes(List<String> syntheticParameterTypes) {
            this.syntheticParameterTypes = syntheticParameterTypes;
            return this;
        }

        public CustomInvokerBuildItem build() {
            return new CustomInvokerBuildItem(this);
        }
    }
}
