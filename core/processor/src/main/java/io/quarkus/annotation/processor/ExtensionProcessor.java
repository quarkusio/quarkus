package io.quarkus.annotation.processor;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public interface ExtensionProcessor {

    void init(Config config, Utils utils);

    void process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);

    void finalizeProcessing();

}
