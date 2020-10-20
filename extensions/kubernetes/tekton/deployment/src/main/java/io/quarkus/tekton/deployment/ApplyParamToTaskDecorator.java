package io.quarkus.tekton.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.tekton.decorator.NamedTaskDecorator;
import io.dekorate.tekton.decorator.TaskProvidingDecorator;
import io.fabric8.tekton.pipeline.v1beta1.TaskSpecFluent;

/**
 * Adds a param to a task.
 * Similar to {@link AddParamToTaskDecorator} but is meant to be executed at a later point, so it can replace values added by
 * it.
 */
public class ApplyParamToTaskDecorator extends NamedTaskDecorator {

    private final String name;
    private final String description;
    private final String defaultValue;

    public ApplyParamToTaskDecorator(String taskName, String name, String description, String defaultValue) {
        super(taskName);
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    @Override
    public void andThenVisit(TaskSpecFluent<?> taskSpec) {
        taskSpec.removeMatchingFromParams(p -> name.equals(p.getName()));
        taskSpec.addNewParam().withName(name).withDescription(description).withNewDefault().withStringVal(defaultValue)
                .endDefault().endParam();
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, TaskProvidingDecorator.class };
    }
}
