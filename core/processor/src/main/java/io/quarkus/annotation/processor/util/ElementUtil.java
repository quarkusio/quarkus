package io.quarkus.annotation.processor.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

public class ElementUtil {

    private static final Pattern REMOVE_LEADING_SPACE = Pattern.compile("^ ", Pattern.MULTILINE);

    private final ProcessingEnvironment processingEnv;

    ElementUtil(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public String getQualifiedName(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "boolean";
            case BYTE:
                return "byte";
            case CHAR:
                return "char";
            case DOUBLE:
                return "double";
            case FLOAT:
                return "float";
            case INT:
                return "int";
            case LONG:
                return "long";
            case SHORT:
                return "short";
            case DECLARED:
                return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
            default:
                // note that it includes annotations, which is something we don't want
                // thus why all this additional work above...
                // this default should never be triggered AFAIK, it's there to be extra safe
                return type.toString();
        }
    }

    public String getBinaryName(TypeElement clazz) {
        return processingEnv.getElementUtils().getBinaryName(clazz).toString();
    }

    public String getRelativeBinaryName(TypeElement typeElement) {
        return buildRelativeBinaryName(typeElement, new StringBuilder()).toString();
    }

    StringBuilder buildRelativeBinaryName(TypeElement typeElement, StringBuilder builder) {
        final Element enclosing = typeElement.getEnclosingElement();
        if (enclosing instanceof TypeElement) {
            buildRelativeBinaryName((TypeElement) enclosing, builder);
            builder.append('$');
        }
        builder.append(typeElement.getSimpleName());
        return builder;
    }

    public String simplifyGenericType(TypeMirror typeMirror) {
        DeclaredType declaredType = ((DeclaredType) typeMirror);
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        String simpleName = declaredType.asElement().getSimpleName().toString();
        if (typeArguments.isEmpty()) {
            return simpleName;
        } else if (typeArguments.size() == 1) {
            return String.format("%s<%s>", simpleName, simplifyGenericType(typeArguments.get(0)));
        } else if (typeArguments.size() == 2) {
            return String.format("%s<%s,%s>", simpleName, simplifyGenericType(typeArguments.get(0)),
                    simplifyGenericType(typeArguments.get(1)));
        }

        return "unknown"; // we should not reach here
    }

    public Map<String, AnnotationMirror> getAnnotations(Element element) {
        return element.getAnnotationMirrors().stream()
                .collect(Collectors.toMap(a -> ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName().toString(),
                        Function.identity()));
    }

    public Map<String, Object> getAnnotationValues(AnnotationMirror annotation) {
        return annotation.getElementValues().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString().substring(0, e.getKey().toString().length() - 2),
                        e -> e.getValue().getValue()));
    }

    public PackageElement getPackageOf(TypeElement clazz) {
        return processingEnv.getElementUtils().getPackageOf(clazz);
    }

    public Name getPackageName(TypeElement clazz) {
        return getPackageOf(clazz).getQualifiedName();
    }

    public TypeElement getClassOf(Element e) {
        Element t = e;
        while (!(t instanceof TypeElement)) {
            if (t == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Element " + e + " has no enclosing class");
                return null;
            }
            t = t.getEnclosingElement();
        }
        return (TypeElement) t;
    }

    public boolean isAnnotationPresent(Element element, String... annotationNames) {
        Set<String> annotations = Set.of(annotationNames);
        for (AnnotationMirror i : element.getAnnotationMirrors()) {
            String annotationName = ((TypeElement) i.getAnnotationType()
                    .asElement()).getQualifiedName()
                    .toString();
            if (annotations.contains(annotationName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is less than ideal but it's the only way I found to detect if a class is local or not.
     * <p>
     * It is important because, while we can scan the annotations of classes in the classpath, we cannot get their javadoc,
     * which in the case of config doc generation is problematic.
     */
    public boolean isLocalClass(TypeElement clazz) {
        try {
            while (clazz.getNestingKind().isNested()) {
                clazz = (TypeElement) clazz.getEnclosingElement();
            }

            processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH, "",
                    clazz.getQualifiedName().toString().replace('.', '/') + ".java");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<String> getJavadoc(Element e) {
        String docComment = processingEnv.getElementUtils().getDocComment(e);

        if (docComment == null || docComment.isBlank()) {
            return Optional.empty();
        }

        // javax.lang.model keeps the leading space after the "*" so we need to remove it.

        return Optional.of(REMOVE_LEADING_SPACE.matcher(docComment)
                .replaceAll("")
                .trim());
    }

    public void addMissingJavadocError(Element e) {
        String error = "Unable to find javadoc for config item " + e.getEnclosingElement() + " " + e;

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, e);
        throw new IllegalStateException(error);
    }

    public boolean isJdkClass(TypeElement e) {
        return e.getQualifiedName().toString().startsWith("java.");
    }
}
