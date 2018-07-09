package org.jboss.shamrock.reflection;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

public interface ReflectionContext {

    RuntimeReflection createClassInvoker(String className);

    ConstructorHandle getConstructor(MethodInfo ctor);

    MethodHandle getMethod(MethodInfo method);

    FieldHandle getField(FieldInfo field);

}
