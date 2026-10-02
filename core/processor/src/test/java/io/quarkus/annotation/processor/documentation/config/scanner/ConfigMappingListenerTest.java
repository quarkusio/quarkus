package io.quarkus.annotation.processor.documentation.config.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.discovery.DiscoveryConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

public class ConfigMappingListenerTest {

    @Test
    public void onConfigRootWithEclipseCompilerMemberNames() {
        // ECJ (used by Eclipse and VS Code) renders annotation members with their full signature
        TypeElement configRoot = configRoot(
                annotation(Types.ANNOTATION_CONFIG_ROOT,
                        Map.of(member("phase", "public abstract io.quarkus.runtime.annotations.ConfigPhase phase() "),
                                value(enumConstant("RUN_TIME")))),
                annotation(Types.ANNOTATION_CONFIG_MAPPING,
                        Map.of(member("prefix", "public abstract java.lang.String prefix() "), value("quarkus.foo"))),
                annotation(Types.ANNOTATION_CONFIG_DOC_PREFIX,
                        Map.of(member("value", "public abstract java.lang.String value() "), value("quarkus.doc-foo"))),
                annotation(Types.ANNOTATION_CONFIG_DOC_FILE_NAME,
                        Map.of(member("value", "public abstract java.lang.String value() "), value("quarkus-doc-foo.adoc"))));

        DiscoveryConfigRoot discoveryConfigRoot = listener().onConfigRoot(configRoot).orElseThrow();

        assertThat(discoveryConfigRoot.getPhase()).isEqualTo(ConfigPhase.RUN_TIME);
        assertThat(discoveryConfigRoot.getPrefix()).isEqualTo("quarkus.foo");
        assertThat(discoveryConfigRoot.getOverriddenDocPrefix()).isEqualTo("quarkus.doc-foo");
        assertThat(discoveryConfigRoot.getOverriddenDocFileName()).isEqualTo("quarkus-doc-foo.adoc");
    }

    @Test
    public void onConfigRootWithDefaults() {
        TypeElement configRoot = configRoot(
                annotation(Types.ANNOTATION_CONFIG_ROOT, Map.of()),
                annotation(Types.ANNOTATION_CONFIG_MAPPING, Map.of()));

        DiscoveryConfigRoot discoveryConfigRoot = listener().onConfigRoot(configRoot).orElseThrow();

        assertThat(discoveryConfigRoot.getPhase()).isEqualTo(ConfigPhase.BUILD_TIME);
        assertThat(discoveryConfigRoot.getPrefix()).isEqualTo("quarkus");
        assertThat(discoveryConfigRoot.getOverriddenDocPrefix()).isNull();
        assertThat(discoveryConfigRoot.getOverriddenDocFileName()).isNull();
    }

    private static ConfigMappingListener listener() {
        Extension extension = Extension.of("org.acme", "foo", "Foo", Extension.NameSource.POM_XML, null, false);
        Config config = new Config(
                ExtensionModule.of("org.acme", "foo", ExtensionModule.ExtensionModuleType.RUNTIME, extension), false);
        Elements elements = proxy(Elements.class, Map.of("getBinaryName", name("org.acme.FooConfig")), "elements");
        ProcessingEnvironment processingEnv = proxy(ProcessingEnvironment.class, Map.of("getElementUtils", elements),
                "processingEnv");
        return new ConfigMappingListener(config, new Utils(processingEnv), new ConfigCollector());
    }

    private static TypeElement configRoot(AnnotationMirror... annotations) {
        return proxy(TypeElement.class, Map.of(
                "getAnnotationMirrors", List.of(annotations),
                "getSimpleName", name("FooConfig"),
                "getQualifiedName", name("org.acme.FooConfig")), "org.acme.FooConfig");
    }

    private static AnnotationMirror annotation(String annotationName,
            Map<ExecutableElement, AnnotationValue> elementValues) {
        TypeElement annotationElement = proxy(TypeElement.class, Map.of("getQualifiedName", name(annotationName)),
                annotationName);
        DeclaredType annotationType = proxy(DeclaredType.class,
                Map.of("getKind", TypeKind.DECLARED, "asElement", annotationElement), annotationName);
        return proxy(AnnotationMirror.class, Map.of(
                "getAnnotationType", annotationType,
                "getElementValues", new LinkedHashMap<>(elementValues)), "@" + annotationName);
    }

    private static ExecutableElement member(String simpleName, String toString) {
        return proxy(ExecutableElement.class, Map.of("getSimpleName", name(simpleName)), toString);
    }

    private static VariableElement enumConstant(String name) {
        return proxy(VariableElement.class, Map.of(), name);
    }

    private static AnnotationValue value(Object value) {
        return proxy(AnnotationValue.class, Map.of("getValue", value), String.valueOf(value));
    }

    private static Name name(String name) {
        return proxy(Name.class, Map.of(), name);
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> results, String toString) {
        return type.cast(Proxy.newProxyInstance(ConfigMappingListenerTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> toString;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> {
                        if (results.containsKey(method.getName())) {
                            yield results.get(method.getName());
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
                }));
    }
}
