package io.quarkus.annotation.processor.documentation.config.model;

public interface ConfigItemVisitor {

    public void visit(AbstractConfigItem configItem);
}
