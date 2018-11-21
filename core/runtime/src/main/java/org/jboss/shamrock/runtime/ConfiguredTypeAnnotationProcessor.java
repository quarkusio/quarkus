package org.jboss.shamrock.runtime;

import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.typesIn;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Annotation processor that saves the javadoc for @ConfiguredType classes so it can be accessed by the build annotation
 * processor in later modules
 */
public class ConfiguredTypeAnnotationProcessor extends AbstractProcessor {


    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ConfiguredType.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver() && !annotations.isEmpty()) {
            doProcess(annotations, roundEnv);
        }
        return true;
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
    }


    public void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        //Call jboss logging tools
        //create a set of classes, and map this to the build step methods
        for (TypeElement annotation : annotations) {
            if (annotation.getQualifiedName().toString().equals(ConfiguredType.class.getName())) {
                for (TypeElement i : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                    Properties properties = new Properties();
                    for (VariableElement field : fieldsIn(i.getEnclosedElements())) {
                        if (field.getAnnotation(ConfigProperty.class) != null) {
                            String docComment = processingEnv.getElementUtils().getDocComment(field);
                            if(docComment == null) {
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to find javadoc for config property " + field);
                                throw new RuntimeException("Unable to find javadoc for config property " + field);
                            }
                            properties.put(field.getSimpleName().toString(), docComment);
                        }
                    }
                    if (!properties.isEmpty()) {
                        try {
                            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", i.getQualifiedName().toString().replace(".", "/") + ".confjavadoc");
                            try (Writer writer = file.openWriter()) {
                                properties.store(writer, "");
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

    }
}
