package io.quarkus.arc.processor;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public record AsyncHandlerInfo(ClassInfo clazz, DotName asyncType, boolean returnType) {
    public boolean matches(MethodInfo method) {
        if (returnType) {
            return method.returnType().name().equals(asyncType);
        } else {
            int matching = 0;
            for (Type parameterType : method.parameterTypes()) {
                if (parameterType.name().equals(asyncType)) {
                    matching++;
                }
            }
            // the invoker is async only in case of exactly one match
            return matching == 1;
        }
    }
}
