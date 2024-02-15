package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.Constants.ANNOTATION_CONFIG_GROUP;
import static io.quarkus.annotation.processor.generate_doc.Constants.ANNOTATION_CONFIG_MAPPING;
import static javax.lang.model.util.ElementFilter.typesIn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import io.quarkus.annotation.processor.ExtensionProcessor;
import io.quarkus.annotation.processor.Outputs;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class LegacyConfigDocExtensionProcessor implements ExtensionProcessor {

    private Utils utils;
    private final ConfigDocItemScanner configDocItemScanner;
    private final ConfigDocWriter configDocWriter;

    private final Map<String, Boolean> annotationUsageTracker = new ConcurrentHashMap<>();
    private boolean configMappingUsed;

    public LegacyConfigDocExtensionProcessor() {
        this.configDocItemScanner = new ConfigDocItemScanner();
        this.configDocWriter = new ConfigDocWriter();
    }

    @Override
    public void init(Config config, Utils utils) {
        this.utils = utils;
    }

    @Override
    public void process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        configMappingUsed = annotations.stream()
                .anyMatch(a -> a.getQualifiedName().toString().equals(Types.ANNOTATION_CONFIG_MAPPING));

        for (TypeElement annotation : annotations) {
            switch (annotation.getQualifiedName().toString()) {
                case Types.ANNOTATION_CONFIG_ROOT:
                    trackAnnotationUsed(Types.ANNOTATION_CONFIG_ROOT);
                    processConfigRoot(roundEnv, annotation);
                    break;
                case Types.ANNOTATION_CONFIG_GROUP:
                    trackAnnotationUsed(Types.ANNOTATION_CONFIG_GROUP);
                    processConfigGroup(roundEnv, annotation);
                    break;
                case Types.ANNOTATION_CONFIG_MAPPING:
                    configMappingUsed = true;
                    break;
            }
        }
    }

    private void processConfigRoot(RoundEnvironment roundEnv, TypeElement annotation) {
        for (TypeElement clazz : typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            final PackageElement pkg = utils.element().getPackageOf(clazz);
            if (pkg == null) {
                utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + clazz + " has no enclosing package");
                continue;
            }

            configDocItemScanner.addConfigRoot(pkg, clazz);
        }
    }

    private void processConfigGroup(RoundEnvironment roundEnv, TypeElement annotation) {
        final Set<String> groupClassNames = new HashSet<>();
        for (TypeElement i : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            if (groupClassNames.add(i.getQualifiedName().toString())) {
                configDocItemScanner.addConfigGroups(i);
            }
        }
    }

    private void recordMappingJavadoc(TypeElement clazz) {
        String className = clazz.getQualifiedName().toString();
        if (!utils.element().isAnnotationPresent(clazz, ANNOTATION_CONFIG_MAPPING)) {
            configDocItemScanner.addConfigGroups(clazz);
        }
        Properties javadocProps = new Properties();

        for (Element e : clazz.getEnclosedElements()) {
            switch (e.getKind()) {
                case INTERFACE: {
                    recordMappingJavadoc(((TypeElement) e));
                    break;
                }

                case METHOD: {
                    if (!isConfigMappingMethodIgnored(e)) {
                        processMethodConfigMapping((ExecutableElement) e, javadocProps, className);
                    }
                    break;
                }
                default:
            }
        }
    }

    private void processMethodConfigMapping(ExecutableElement method, Properties javadocProps, String className) {
        if (method.getModifiers().contains(Modifier.ABSTRACT)) {
            TypeMirror returnType = method.getReturnType();
            if (TypeKind.DECLARED.equals(returnType.getKind())) {
                DeclaredType declaredType = (DeclaredType) returnType;
                if (!utils.element().isAnnotationPresent(declaredType.asElement(), ANNOTATION_CONFIG_GROUP)) {
                    TypeElement type = unwrapConfigGroup(returnType);
                    if (type != null && ElementKind.INTERFACE.equals(type.getKind())) {
                        recordMappingJavadoc(type);
                        configDocItemScanner.addConfigGroups(type);
                    }
                }
            }
        }
    }

    private TypeElement unwrapConfigGroup(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return null;
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        String name = declaredType.asElement()
                .toString();
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            if (!name.startsWith("java.")) {
                return (TypeElement) declaredType.asElement();
            }
        } else if (typeArguments.size() == 1) {
            if (name.equals(Optional.class.getName()) ||
                    name.equals(List.class.getName()) ||
                    name.equals(Set.class.getName())) {
                return unwrapConfigGroup(typeArguments.get(0));
            }
        } else if (typeArguments.size() == 2) {
            if (name.equals(Map.class.getName())) {
                return unwrapConfigGroup(typeArguments.get(1));
            }
        }
        return null;
    }

    private static boolean isConfigMappingMethodIgnored(Element element) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            String annotationName = ((TypeElement) annotationMirror.getAnnotationType().asElement())
                    .getQualifiedName().toString();
            if (Constants.ANNOTATION_CONFIG_DOC_IGNORE.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private void trackAnnotationUsed(String annotation) {
        annotationUsageTracker.put(annotation, true);
    }

    @Override
    public void finalizeProcessing() {
        try {
            // not ideal but let's load the javadoc properties (we can't use the filer API here as we can't open the same file twice)...
            Properties javadocProperties = new Properties();
            Path javadocPropertiesPath = utils.filer().getTargetPath().resolve("classes")
                    .resolve(Outputs.META_INF_QUARKUS_JAVADOC);
            if (Files.isReadable(javadocPropertiesPath)) {
                try (InputStream is = Files.newInputStream(javadocPropertiesPath)) {
                    javadocProperties.load(is);
                }
            }

            final Set<ConfigDocGeneratedOutput> outputs = configDocItemScanner
                    .scanExtensionsConfigurationItems(javadocProperties, configMappingUsed);
            for (ConfigDocGeneratedOutput output : outputs) {
                DocGeneratorUtil.sort(output.getConfigDocItems()); // sort before writing
                configDocWriter.writeAllExtensionConfigDocumentation(output);
            }
        } catch (IOException e) {
            utils.processingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate extension doc: " + e);
            return;
        }
    }

}
