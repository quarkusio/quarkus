package io.quarkus.panache.common.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * An annotation processor that is used to generate a marker file, to tell Quarkus that this archive has a dependency on
 * Panache entities, and therefore may need to be transformed for enhanced field access.
 *
 * This works because any archive that depends on Panache will have a transitive dependency on panache-common, which
 * contains this processor. The Javac compiler will then run this processor and create the marker file.
 */
public class PanacheAnnotationProcessor implements Processor {

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        try {
            FileObject marker = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    "META-INF/panache-archive.marker");
            try (OutputStream out = marker.openOutputStream()) {
                out.write(
                        "This file is a marker, it exists to tell Quarkus that this archive has a dependency on Panache, and may need to be transformed at build time"
                                .getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        return Collections.emptyList();
    }
}
