package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 * Use this configurator to construct an {@link AnnotationInstance} that represents a CDI qualifier.
 * <p>
 * This construct is not thread-safe.
 * 
 * @param <C>
 * @see BeanConfigurator
 * @see ObserverConfigurator
 */
public final class QualifierConfigurator<C extends Consumer<AnnotationInstance>> {

    private final C qualifierConsumer;
    private final List<AnnotationValue> values;
    private DotName annotationName;

    QualifierConfigurator(C qualifierConsumer) {
        this.qualifierConsumer = qualifierConsumer;
        this.values = new ArrayList<>();
    }

    public QualifierConfigurator<C> annotation(Class<? extends Annotation> annotationClass) {
        this.annotationName = DotName.createSimple(annotationClass.getName());
        return this;
    }

    public QualifierConfigurator<C> annotation(DotName annotationName) {
        this.annotationName = annotationName;
        return this;
    }

    public QualifierConfigurator<C> addValue(String name, Object value) {
        values.add(createAnnotationValue(name, value));
        return this;
    }

    public C done() {
        qualifierConsumer.accept(AnnotationInstance.create(annotationName, null, values));
        return qualifierConsumer;
    }

    @SuppressWarnings("unchecked")
    static AnnotationValue createAnnotationValue(String name, Object val) {
        if (val instanceof String) {
            return AnnotationValue.createStringValue(name, val.toString());
        } else if (val instanceof Integer) {
            return AnnotationValue.createIntegerValue(name, (int) val);
        } else if (val instanceof Long) {
            return AnnotationValue.createLongalue(name, (long) val);
        } else if (val instanceof Byte) {
            return AnnotationValue.createByteValue(name, (byte) val);
        } else if (val instanceof Float) {
            return AnnotationValue.createFloatValue(name, (float) val);
        } else if (val instanceof Double) {
            return AnnotationValue.createDouleValue(name, (double) val);
        } else if (val instanceof Short) {
            return AnnotationValue.createShortValue(name, (short) val);
        } else if (val instanceof Boolean) {
            return AnnotationValue.createBooleanValue(name, (boolean) val);
        } else if (val instanceof Character) {
            return AnnotationValue.createCharacterValue(name, (char) val);
        } else if (val instanceof Enum) {
            return AnnotationValue.createEnumValue(name, DotName.createSimple(val.getClass().getName()), val.toString());
        } else if (val instanceof Class) {
            return AnnotationValue.createClassValue(name,
                    Type.create(DotName.createSimple(((Class<?>) val).getName()), Kind.CLASS));
        } else if (val instanceof List) {
            List<Object> listOfVals = (List<Object>) val;
            AnnotationValue[] values = new AnnotationValue[listOfVals.size()];
            for (int i = 0; i < values.length; i++) {
                values[i] = createAnnotationValue(name, listOfVals.get(i));
            }
            return AnnotationValue.createArrayValue(name, values);
        } else if (val.getClass().isArray()) {
            AnnotationValue[] values = new AnnotationValue[Array.getLength(val)];
            for (int i = 0; i < values.length; i++) {
                values[i] = createAnnotationValue(name, Array.get(val, i));
            }
            return AnnotationValue.createArrayValue(name, values);
        }
        throw new IllegalArgumentException("Unsupported value type for [" + name + "]: " + val);
    }

}
