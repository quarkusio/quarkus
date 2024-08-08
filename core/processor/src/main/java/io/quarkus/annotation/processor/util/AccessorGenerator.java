package io.quarkus.annotation.processor.util;

import static javax.lang.model.util.ElementFilter.constructorsIn;
import static javax.lang.model.util.ElementFilter.fieldsIn;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.jboss.jdeparser.FormatPreferences;
import org.jboss.jdeparser.JAssignableExpr;
import org.jboss.jdeparser.JCall;
import org.jboss.jdeparser.JClassDef;
import org.jboss.jdeparser.JDeparser;
import org.jboss.jdeparser.JExprs;
import org.jboss.jdeparser.JFiler;
import org.jboss.jdeparser.JMethodDef;
import org.jboss.jdeparser.JMod;
import org.jboss.jdeparser.JSourceFile;
import org.jboss.jdeparser.JSources;
import org.jboss.jdeparser.JType;
import org.jboss.jdeparser.JTypes;

public final class AccessorGenerator {

    private static final String QUARKUS_GENERATED = "io.quarkus.Generated";
    private static final String INSTANCE_SYM = "__instance";

    private final ProcessingEnvironment processingEnv;
    private final ElementUtil elementUtil;
    private final Set<String> generatedAccessors = new ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);

    AccessorGenerator(ProcessingEnvironment processingEnv, ElementUtil elementUtil) {
        this.processingEnv = processingEnv;
        this.elementUtil = elementUtil;
    }

    public void generateAccessor(final TypeElement clazz) {
        if (!generatedAccessors.add(clazz.getQualifiedName().toString())) {
            return;
        }
        final FormatPreferences fp = new FormatPreferences();
        final JSources sources = JDeparser.createSources(JFiler.newInstance(processingEnv.getFiler()), fp);
        final PackageElement packageElement = elementUtil.getPackageOf(clazz);
        final String className = elementUtil.buildRelativeBinaryName(clazz, new StringBuilder()).append("$$accessor")
                .toString();
        final JSourceFile sourceFile = sources.createSourceFile(packageElement.getQualifiedName()
                .toString(), className);
        JType clazzType = JTypes.typeOf(clazz.asType());
        if (clazz.asType() instanceof DeclaredType) {
            DeclaredType declaredType = ((DeclaredType) clazz.asType());
            TypeMirror enclosingType = declaredType.getEnclosingType();
            if (enclosingType != null && enclosingType.getKind() == TypeKind.DECLARED
                    && clazz.getModifiers()
                            .contains(Modifier.STATIC)) {
                // Ugly workaround for Eclipse APT and static nested types
                clazzType = unnestStaticNestedType(declaredType);
            }
        }
        final JClassDef classDef = sourceFile._class(JMod.PUBLIC | JMod.FINAL, className);
        classDef.constructor(JMod.PRIVATE); // no construction
        classDef.annotate(QUARKUS_GENERATED)
                .value("Quarkus annotation processor");
        final JAssignableExpr instanceName = JExprs.name(INSTANCE_SYM);
        boolean isEnclosingClassPublic = clazz.getModifiers()
                .contains(Modifier.PUBLIC);
        // iterate fields
        boolean generationNeeded = false;
        for (VariableElement field : fieldsIn(clazz.getEnclosedElements())) {
            final Set<Modifier> mods = field.getModifiers();
            if (mods.contains(Modifier.PRIVATE) || mods.contains(Modifier.STATIC) || mods.contains(Modifier.FINAL)) {
                // skip it
                continue;
            }
            final TypeMirror fieldType = field.asType();
            if (mods.contains(Modifier.PUBLIC) && isEnclosingClassPublic) {
                // we don't need to generate a method accessor when the following conditions are met:
                // 1) the field is public
                // 2) the enclosing class is public
                // 3) the class type of the field is public
                if (fieldType instanceof DeclaredType) {
                    final DeclaredType declaredType = (DeclaredType) fieldType;
                    final TypeElement typeElement = (TypeElement) declaredType.asElement();
                    if (typeElement.getModifiers()
                            .contains(Modifier.PUBLIC)) {
                        continue;
                    }
                } else {
                    continue;
                }

            }
            generationNeeded = true;

            final JType realType = JTypes.typeOf(fieldType);
            final JType publicType = fieldType instanceof PrimitiveType ? realType : JType.OBJECT;

            final String fieldName = field.getSimpleName()
                    .toString();
            final JMethodDef getter = classDef.method(JMod.PUBLIC | JMod.STATIC, publicType, "get_" + fieldName);
            getter.annotate(SuppressWarnings.class)
                    .value("unchecked");
            getter.param(JType.OBJECT, INSTANCE_SYM);
            getter.body()
                    ._return(instanceName.cast(clazzType)
                            .field(fieldName));
            final JMethodDef setter = classDef.method(JMod.PUBLIC | JMod.STATIC, JType.VOID, "set_" + fieldName);
            setter.annotate(SuppressWarnings.class)
                    .value("unchecked");
            setter.param(JType.OBJECT, INSTANCE_SYM);
            setter.param(publicType, fieldName);
            final JAssignableExpr fieldExpr = JExprs.name(fieldName);
            setter.body()
                    .assign(instanceName.cast(clazzType)
                            .field(fieldName),
                            (publicType.equals(realType) ? fieldExpr : fieldExpr.cast(realType)));
        }

        // we need to generate an accessor if the class isn't public
        if (!isEnclosingClassPublic) {
            for (ExecutableElement ctor : constructorsIn(clazz.getEnclosedElements())) {
                if (ctor.getModifiers()
                        .contains(Modifier.PRIVATE)) {
                    // skip it
                    continue;
                }
                generationNeeded = true;
                StringBuilder b = new StringBuilder();
                for (VariableElement parameter : ctor.getParameters()) {
                    b.append('_');
                    b.append(parameter.asType()
                            .toString()
                            .replace('.', '_'));
                }
                String codedName = b.toString();
                final JMethodDef ctorMethod = classDef.method(JMod.PUBLIC | JMod.STATIC, JType.OBJECT, "construct" + codedName);
                final JCall ctorCall = clazzType._new();
                for (VariableElement parameter : ctor.getParameters()) {
                    final TypeMirror paramType = parameter.asType();
                    final JType realType = JTypes.typeOf(paramType);
                    final JType publicType = paramType instanceof PrimitiveType ? realType : JType.OBJECT;
                    final String name = parameter.getSimpleName()
                            .toString();
                    ctorMethod.param(publicType, name);
                    final JAssignableExpr nameExpr = JExprs.name(name);
                    ctorCall.arg(publicType.equals(realType) ? nameExpr : nameExpr.cast(realType));
                }
                ctorMethod.body()
                        ._return(ctorCall);
            }
        }

        // if no constructor or field access is needed, don't generate anything
        if (generationNeeded) {
            try {
                sources.writeSources();
            } catch (IOException e) {
                processingEnv.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR, "Failed to generate source file: " + e, clazz);
            }
        }
    }

    private JType unnestStaticNestedType(DeclaredType declaredType) {
        final TypeElement typeElement = (TypeElement) declaredType.asElement();

        final String name = typeElement.getQualifiedName()
                .toString();
        final JType rawType = JTypes.typeNamed(name);
        final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return rawType;
        }
        JType[] args = new JType[typeArguments.size()];
        for (int i = 0; i < typeArguments.size(); i++) {
            final TypeMirror argument = typeArguments.get(i);
            args[i] = JTypes.typeOf(argument);
        }
        return rawType.typeArg(args);
    }
}
