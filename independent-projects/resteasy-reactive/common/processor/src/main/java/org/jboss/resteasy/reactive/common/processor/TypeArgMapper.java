package org.jboss.resteasy.reactive.common.processor;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OBJECT;

import java.util.List;
import java.util.function.Function;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

public class TypeArgMapper implements Function<String, Type> {
    private final ClassInfo declaringClass;
    private final IndexView index;

    public TypeArgMapper(ClassInfo declaringClass, IndexView index) {
        this.declaringClass = declaringClass;
        this.index = index;
    }

    @Override
    public Type apply(String v) {
        // we attempt to resolve type variables
        ClassInfo declarer = declaringClass;
        int pos = -1;
        for (;;) {
            if (declarer == null || OBJECT.equals(declarer.name())) {
                return null;
            }
            List<TypeVariable> typeParameters = declarer.typeParameters();
            for (int i = 0; i < typeParameters.size(); i++) {
                TypeVariable tv = typeParameters.get(i);
                if (tv.identifier().equals(v)) {
                    pos = i;
                }
            }
            if (pos != -1) {
                break;
            }
            declarer = index.getClassByName(declarer.superName());
        }
        Type type = JandexUtil.resolveTypeParameters(declaringClass.name(), declarer.name(), index).get(pos);
        if (type.kind() == Type.Kind.TYPE_VARIABLE && type.asTypeVariable().identifier().equals(v)) {
            List<Type> bounds = type.asTypeVariable().bounds();
            if (bounds.isEmpty()) {
                return ClassType.OBJECT_TYPE;
            }
            return bounds.get(0);
        } else {
            return type;
        }
    }
}
