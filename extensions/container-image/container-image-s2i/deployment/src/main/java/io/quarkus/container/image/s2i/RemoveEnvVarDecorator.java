package io.quarkus.container.image.s2i;

import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;

public class RemoveEnvVarDecorator extends ApplicationContainerDecorator<ContainerFluent<?>> {

    private final String envVarName;

    public RemoveEnvVarDecorator(String envVarName) {
        this(ANY, envVarName);
    }

    public RemoveEnvVarDecorator(String name, String envVarName) {
        super(name);
        this.envVarName = envVarName;
    }

    public void andThenVisit(ContainerFluent<?> container) {
        container.removeMatchingFromEnv(e -> e.getName().equals(envVarName));
    }

    public String getEnvVarKey() {
        return this.envVarName;
    }

    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddEnvVarDecorator.class };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((envVarName == null) ? 0 : envVarName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RemoveEnvVarDecorator other = (RemoveEnvVarDecorator) obj;
        if (envVarName == null) {
            if (other.envVarName != null)
                return false;
        } else if (!envVarName.equals(other.envVarName))
            return false;
        return true;
    }
}
