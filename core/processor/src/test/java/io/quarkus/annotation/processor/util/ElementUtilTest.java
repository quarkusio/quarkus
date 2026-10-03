package io.quarkus.annotation.processor.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;

import org.junit.jupiter.api.Test;

public class ElementUtilTest {

    @Test
    public void getAnnotationValuesWithJavacMemberNames() {
        AnnotationMirror annotation = annotation(Map.of(member("prefix", "prefix()"), value("quarkus.foo")));

        assertThat(new ElementUtil(null).getAnnotationValues(annotation))
                .containsExactly(Map.entry("prefix", "quarkus.foo"));
    }

    @Test
    public void getAnnotationValuesWithEclipseCompilerMemberNames() {
        // ECJ (used by Eclipse and VS Code) renders annotation members with their full signature
        AnnotationMirror annotation = annotation(
                Map.of(member("prefix", "public abstract java.lang.String prefix() "), value("quarkus.foo")));

        assertThat(new ElementUtil(null).getAnnotationValues(annotation))
                .containsExactly(Map.entry("prefix", "quarkus.foo"));
    }

    private static AnnotationMirror annotation(Map<ExecutableElement, AnnotationValue> elementValues) {
        Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>(elementValues);
        return proxy(AnnotationMirror.class, "getElementValues", values, "annotation");
    }

    private static ExecutableElement member(String simpleName, String toString) {
        return proxy(ExecutableElement.class, "getSimpleName", name(simpleName), toString);
    }

    private static AnnotationValue value(Object value) {
        return proxy(AnnotationValue.class, "getValue", value, String.valueOf(value));
    }

    private static Name name(String name) {
        return (Name) Proxy.newProxyInstance(ElementUtilTest.class.getClassLoader(), new Class<?>[] { Name.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "contentEquals" -> name.contentEquals((CharSequence) args[0]);
                    case "toString" -> name;
                    case "length" -> name.length();
                    case "charAt" -> name.charAt((int) args[0]);
                    case "subSequence" -> name.subSequence((int) args[0], (int) args[1]);
                    case "hashCode" -> name.hashCode();
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static <T> T proxy(Class<T> type, String getter, Object result, String toString) {
        return type.cast(Proxy.newProxyInstance(ElementUtilTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> switch (method.getName()) {
                    case "toString" -> toString;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> {
                        if (method.getName().equals(getter)) {
                            yield result;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    }
                }));
    }
}
