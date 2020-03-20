
package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.deps.kubernetes.api.builder.Predicate;
import io.dekorate.deps.kubernetes.api.model.ContainerBuilder;
import io.dekorate.deps.kubernetes.api.model.ContainerFluent.EnvFromNested;
import io.dekorate.deps.kubernetes.api.model.ContainerFluent.EnvNested;
import io.dekorate.deps.kubernetes.api.model.EnvFromSourceBuilder;
import io.dekorate.deps.kubernetes.api.model.EnvVarBuilder;
import io.dekorate.deps.kubernetes.api.model.EnvVarFluent.ValueFromNested;
import io.dekorate.doc.Description;
import io.dekorate.kubernetes.config.Env;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.utils.Strings;

@Description("Add a environment variable to the container.")
public class ApplyEnvVarDecorator extends ApplicationContainerDecorator<ContainerBuilder> {

    private final Env env;

    public ApplyEnvVarDecorator(Env env) {
        this(ANY, ANY, env);
    }

    public ApplyEnvVarDecorator(String deployment, String container, Env env) {
        super(deployment, container);
        this.env = env;
    }

    public void andThenVisit(ContainerBuilder builder) {
        Predicate<EnvVarBuilder> matchingEnv = new Predicate<EnvVarBuilder>() {
            public Boolean apply(EnvVarBuilder e) {
                if (e.getName() != null) {
                    return e.getName().equals(env.getName());
                }
                return false;
            }
        };

        Predicate<EnvFromSourceBuilder> matchingEnvFrom = new Predicate<EnvFromSourceBuilder>() {
            public Boolean apply(EnvFromSourceBuilder e) {
                if (e.getSecretRef() != null
                        && e.getSecretRef().getName() != null) {
                    return e.getSecretRef().getName().equals(env.getSecret());
                } else if (e.getConfigMapRef() != null
                        && e.editConfigMapRef().getName() != null) {
                    return e.editConfigMapRef().getName().equals(env.getConfigmap());
                }
                return false;
            }
        };

        builder.removeMatchingFromEnv(matchingEnv);
        builder.removeMatchingFromEnvFrom(matchingEnvFrom);

        if (Strings.isNotNullOrEmpty(this.env.getSecret())) {
            this.populateFromSecret(builder);
        } else if (Strings.isNotNullOrEmpty(this.env.getConfigmap())) {
            this.populateFromConfigMap(builder);
        } else if (Strings.isNotNullOrEmpty(this.env.getField())) {
            this.populateFromField(builder);
        } else if (Strings.isNotNullOrEmpty(this.env.getName())) {
            ((EnvNested) ((EnvNested) builder.addNewEnv().withName(this.env.getName())).withValue(this.env.getValue()))
                    .endEnv();
        }
    }

    private void populateFromSecret(ContainerBuilder builder) {
        if (Strings.isNotNullOrEmpty(this.env.getName()) && Strings.isNotNullOrEmpty(this.env.getValue())) {
            ((EnvNested) ((ValueFromNested) ((EnvNested) builder.addNewEnv().withName(this.env.getName())).withNewValueFrom()
                    .withNewSecretKeyRef(this.env.getValue(), this.env.getSecret(), false)).endValueFrom()).endEnv();
        } else {
            ((EnvFromNested) builder.addNewEnvFrom().withNewSecretRef(this.env.getSecret(), false)).endEnvFrom();
        }
    }

    private void populateFromConfigMap(ContainerBuilder builder) {
        if (Strings.isNotNullOrEmpty(this.env.getName()) && Strings.isNotNullOrEmpty(this.env.getValue())) {
            ((EnvNested) ((ValueFromNested) ((EnvNested) builder.addNewEnv().withName(this.env.getName())).withNewValueFrom()
                    .withNewConfigMapKeyRef(this.env.getValue(), this.env.getConfigmap(), false)).endValueFrom()).endEnv();
        } else {
            ((EnvFromNested) builder.addNewEnvFrom().withNewConfigMapRef(this.env.getConfigmap(), false)).endEnvFrom();
        }
    }

    private void populateFromField(ContainerBuilder builder) {
        ((ValueFromNested) ((EnvNested) builder.addNewEnv().withName(this.env.getName())).withNewValueFrom()
                .withNewFieldRef((String) null, this.env.getField())).endValueFrom();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ApplyEnvVarDecorator addEnvVarDecorator = (ApplyEnvVarDecorator) o;
            return Objects.equals(this.env, addEnvVarDecorator.env);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[] { this.env });
    }
}
