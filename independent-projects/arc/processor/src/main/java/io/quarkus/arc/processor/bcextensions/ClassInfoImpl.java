package io.quarkus.arc.processor.bcextensions;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.declarations.PackageInfo;
import jakarta.enterprise.lang.model.declarations.RecordComponentInfo;
import jakarta.enterprise.lang.model.types.Type;
import jakarta.enterprise.lang.model.types.TypeVariable;

import org.jboss.jandex.DotName;

class ClassInfoImpl extends DeclarationInfoImpl<org.jboss.jandex.ClassInfo> implements ClassInfo {
    ClassInfoImpl(org.jboss.jandex.IndexView jandexIndex, org.jboss.jandex.MutableAnnotationOverlay annotationOverlay,
            org.jboss.jandex.ClassInfo jandexDeclaration) {
        super(jandexIndex, annotationOverlay, jandexDeclaration);
    }

    @Override
    public String name() {
        return jandexDeclaration.name().toString();
    }

    @Override
    public String simpleName() {
        return jandexDeclaration.simpleName();
    }

    @Override
    public PackageInfo packageInfo() {
        String packageName = jandexDeclaration.name().packagePrefix();
        org.jboss.jandex.ClassInfo packageClass = jandexIndex.getClassByName(
                DotName.createSimple(packageName + ".package-info"));
        return new PackageInfoImpl(jandexIndex, annotationOverlay, packageClass);
    }

    @Override
    public List<TypeVariable> typeParameters() {
        return jandexDeclaration.typeParameters()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlay, it))
                .filter(Type::isTypeVariable) // not necessary, just as a precaution
                .map(Type::asTypeVariable) // not necessary, just as a precaution
                .toList();
    }

    @Override
    public Type superClass() {
        org.jboss.jandex.Type jandexSuperType = jandexDeclaration.superClassType();
        if (jandexSuperType == null) {
            return null;
        }
        return TypeImpl.fromJandexType(jandexIndex, annotationOverlay, jandexSuperType);
    }

    @Override
    public ClassInfo superClassDeclaration() {
        DotName jandexSuperType = jandexDeclaration.superName();
        if (jandexSuperType == null) {
            return null;
        }
        return new ClassInfoImpl(jandexIndex, annotationOverlay, jandexIndex.getClassByName(jandexSuperType));
    }

    @Override
    public List<Type> superInterfaces() {
        return jandexDeclaration.interfaceTypes()
                .stream()
                .map(it -> TypeImpl.fromJandexType(jandexIndex, annotationOverlay, it))
                .toList();
    }

    @Override
    public List<ClassInfo> superInterfacesDeclarations() {
        return jandexDeclaration.interfaceNames()
                .stream()
                .map(it -> (ClassInfo) new ClassInfoImpl(jandexIndex, annotationOverlay, jandexIndex.getClassByName(it)))
                .toList();
    }

    @Override
    public boolean isPlainClass() {
        return !isInterface() && !isEnum() && !isAnnotation() && !isRecord();
    }

    @Override
    public boolean isInterface() {
        if (isAnnotation()) {
            return false;
        }
        return Modifier.isInterface(jandexDeclaration.flags());
    }

    @Override
    public boolean isEnum() {
        return jandexDeclaration.isEnum();
    }

    @Override
    public boolean isAnnotation() {
        return jandexDeclaration.isAnnotation();
    }

    @Override
    public boolean isRecord() {
        return jandexDeclaration.isRecord();
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(jandexDeclaration.flags());
    }

    @Override
    public boolean isFinal() {
        return Modifier.isFinal(jandexDeclaration.flags());
    }

    @Override
    public int modifiers() {
        return jandexDeclaration.flags();
    }

    @Override
    public Collection<MethodInfo> constructors() {
        List<MethodInfo> result = new ArrayList<>();
        for (org.jboss.jandex.MethodInfo jandexMethod : jandexDeclaration.methods()) {
            if (jandexMethod.isSynthetic()) {
                continue;
            }
            if (MethodPredicates.IS_CONSTRUCTOR_JANDEX.test(jandexMethod)) {
                result.add(new MethodInfoImpl(jandexIndex, annotationOverlay, jandexMethod));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private List<org.jboss.jandex.ClassInfo> allSupertypes() {
        List<org.jboss.jandex.ClassInfo> result = new ArrayList<>();
        // an interface may be inherited multiple times, but we only want to process it once
        Set<DotName> alreadySeen = new HashSet<>();
        Queue<org.jboss.jandex.ClassInfo> workQueue = new ArrayDeque<>();
        workQueue.add(jandexDeclaration);
        while (!workQueue.isEmpty()) {
            org.jboss.jandex.ClassInfo clazz = workQueue.remove();
            if (alreadySeen.contains(clazz.name())) {
                continue;
            }
            result.add(clazz);
            alreadySeen.add(clazz.name());

            DotName superClassName = clazz.superName();
            if (superClassName != null && !DotNames.OBJECT.equals(superClassName)) {
                org.jboss.jandex.ClassInfo superClass = jandexIndex.getClassByName(superClassName);
                workQueue.add(superClass);
            }
            for (DotName superInterfaceName : clazz.interfaceNames()) {
                org.jboss.jandex.ClassInfo superInterface = jandexIndex.getClassByName(superInterfaceName);
                workQueue.add(superInterface);
            }
        }
        return result;
    }

    @Override
    public Collection<MethodInfo> methods() {
        List<MethodInfo> result = new ArrayList<>();
        for (org.jboss.jandex.ClassInfo clazz : allSupertypes()) {
            for (org.jboss.jandex.MethodInfo jandexMethod : clazz.methods()) {
                if (jandexMethod.isSynthetic()) {
                    continue;
                }
                if (MethodPredicates.IS_METHOD_JANDEX.test(jandexMethod)) {
                    result.add(new MethodInfoImpl(jandexIndex, annotationOverlay, jandexMethod));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<FieldInfo> fields() {
        List<FieldInfo> result = new ArrayList<>();
        for (org.jboss.jandex.ClassInfo clazz : allSupertypes()) {
            for (org.jboss.jandex.FieldInfo jandexField : clazz.fields()) {
                if (jandexField.isSynthetic()) {
                    continue;
                }
                result.add(new FieldInfoImpl(jandexIndex, annotationOverlay, jandexField));
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public Collection<RecordComponentInfo> recordComponents() {
        List<RecordComponentInfo> result = new ArrayList<>();
        for (org.jboss.jandex.RecordComponentInfo recordComponent : jandexDeclaration.recordComponents()) {
            result.add(new RecordComponentInfoImpl(jandexIndex, annotationOverlay, recordComponent));
        }
        return Collections.unmodifiableList(result);
    }
}
