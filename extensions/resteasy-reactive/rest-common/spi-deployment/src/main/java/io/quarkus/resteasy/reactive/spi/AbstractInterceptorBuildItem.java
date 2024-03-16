package io.quarkus.resteasy.reactive.spi;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public abstract class AbstractInterceptorBuildItem extends MultiBuildItem implements CheckBean {

    private final String className;
    private final Integer priority;
    private final boolean registerAsBean;
    private final Set<String> nameBindingNames;

    protected AbstractInterceptorBuildItem(Builder<?, ?> builder) {
        this.className = builder.className;
        this.priority = builder.priority;
        this.registerAsBean = builder.registerAsBean;
        this.nameBindingNames = builder.nameBindingNames;
    }

    protected AbstractInterceptorBuildItem(String className) {
        this.className = className;
        this.priority = null;
        this.registerAsBean = true;
        this.nameBindingNames = Collections.emptySet();
    }

    public String getClassName() {
        return className;
    }

    public Integer getPriority() {
        return priority;
    }

    public Set<String> getNameBindingNames() {
        return nameBindingNames;
    }

    @Override
    public boolean isRegisterAsBean() {
        return registerAsBean;
    }

    public static abstract class Builder<T extends AbstractInterceptorBuildItem, B extends Builder<T, B>> {
        private final String className;

        private Integer priority;
        private boolean registerAsBean = true;
        private Set<String> nameBindingNames = Collections.emptySet();

        public Builder(String className) {
            this.className = className;
        }

        public B setPriority(Integer priority) {
            this.priority = priority;
            return (B) this;
        }

        public B setRegisterAsBean(boolean registerAsBean) {
            this.registerAsBean = registerAsBean;
            return (B) this;
        }

        public B setNameBindingNames(Set<String> nameBindingNames) {
            Objects.requireNonNull(nameBindingNames);
            this.nameBindingNames = nameBindingNames;
            return (B) this;
        }

        public abstract T build();
    }
}
