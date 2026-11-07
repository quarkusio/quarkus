package io.quarkus.jackson.runtime.graal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.NullValueProvider;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.util.ClassUtil;

/**
 * Method handles are not optimized as effectively as reflection in native images.
 */
@TargetClass(className = "tools.jackson.databind.ser.BeanPropertyWriter")
final class Target_tools_jackson_databind_ser_BeanPropertyWriter {

    @Alias
    protected AnnotatedMember _member;

    @Substitute
    public final Object get(Object bean) throws Exception {
        return JacksonReflectionHelper.getValue(_member, bean);
    }
}

@TargetClass(className = "tools.jackson.databind.deser.impl.MethodProperty")
final class Target_tools_jackson_databind_deser_impl_MethodProperty {

    @Alias
    protected AnnotatedMember _annotated;

    @Alias
    protected boolean _skipNulls;

    @Alias
    protected NullValueProvider _nullProvider;

    @Alias
    protected ValueDeserializer<Object> _valueDeserializer;

    @Alias
    protected TypeDeserializer _valueTypeDeserializer;

    @Alias
    protected PropertyName _propName;

    @Alias
    protected JavaType _type;

    @Substitute
    public void deserializeAndSet(JsonParser parser, DeserializationContext context, Object instance) throws JacksonException {
        Object value;
        if (parser.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return;
            }
            value = _nullProvider.getNullValue(context);
        } else if (_valueTypeDeserializer == null) {
            value = _valueDeserializer.deserialize(parser, context);
            if (value == null) {
                if (_skipNulls) {
                    return;
                }
                value = _nullProvider.getNullValue(context);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(parser, context, _valueTypeDeserializer);
        }
        try {
            JacksonReflectionHelper.setValue(_annotated, instance, value);
        } catch (Throwable e) {
            JacksonReflectionHelper.throwAsJacksonException(parser, e, value, _propName, _type);
        }
    }

    @Substitute
    public Object deserializeSetAndReturn(JsonParser parser, DeserializationContext context, Object instance)
            throws JacksonException {
        Object value;
        if (parser.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return instance;
            }
            value = _nullProvider.getNullValue(context);
        } else if (_valueTypeDeserializer == null) {
            value = _valueDeserializer.deserialize(parser, context);
            if (value == null) {
                if (_skipNulls) {
                    return instance;
                }
                value = _nullProvider.getNullValue(context);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(parser, context, _valueTypeDeserializer);
        }
        try {
            Object result = JacksonReflectionHelper.setValue(_annotated, instance, value);
            return result == null ? instance : result;
        } catch (Throwable e) {
            JacksonReflectionHelper.throwAsJacksonException(parser, e, value, _propName, _type);
            return null;
        }
    }

    @Substitute
    public final void set(DeserializationContext context, Object instance, Object value) throws JacksonException {
        if (value == null && _skipNulls) {
            return;
        }
        try {
            JacksonReflectionHelper.setValue(_annotated, instance, value);
        } catch (Throwable e) {
            JacksonReflectionHelper.throwAsJacksonException(context.getParser(), e, value, _propName, _type);
        }
    }

    @Substitute
    public Object setAndReturn(DeserializationContext context, Object instance, Object value) throws JacksonException {
        if (value == null && _skipNulls) {
            return instance;
        }
        try {
            Object result = JacksonReflectionHelper.setValue(_annotated, instance, value);
            return result == null ? instance : result;
        } catch (Throwable e) {
            JacksonReflectionHelper.throwAsJacksonException(context.getParser(), e, value, _propName, _type);
            return null;
        }
    }
}

@TargetClass(className = "tools.jackson.databind.introspect.AnnotatedMethod")
final class Target_tools_jackson_databind_introspect_AnnotatedMethod {

    @Alias
    protected Method _method;

    @Substitute
    public final Object call() throws Exception {
        return JacksonReflectionHelper.invoke(_method, null);
    }

    @Substitute
    public final Object call(Object[] arguments) throws Exception {
        return JacksonReflectionHelper.invoke(_method, null, arguments);
    }

    @Substitute
    public final Object call1(Object argument) throws Exception {
        return JacksonReflectionHelper.invoke(_method, null, argument);
    }

    @Substitute
    public final Object callOn(Object pojo) throws Exception {
        return JacksonReflectionHelper.invoke(_method, pojo);
    }

    @Substitute
    public final Object callOnWith(Object pojo, Object... arguments) throws Exception {
        Object receiver = Modifier.isStatic(_method.getModifiers()) ? null : pojo;
        return JacksonReflectionHelper.invoke(_method, receiver, arguments);
    }
}

@TargetClass(className = "tools.jackson.databind.introspect.AnnotatedConstructor")
final class Target_tools_jackson_databind_introspect_AnnotatedConstructor {

    @Alias
    protected Constructor<?> _constructor;

    @Substitute
    public final Object call() throws Exception {
        return JacksonReflectionHelper.newInstance(_constructor);
    }

    @Substitute
    public final Object call(Object[] arguments) throws Exception {
        return JacksonReflectionHelper.newInstance(_constructor, arguments);
    }

    @Substitute
    public final Object call1(Object argument) throws Exception {
        return JacksonReflectionHelper.newInstance(_constructor, argument);
    }
}

final class JacksonReflectionHelper {

    private JacksonReflectionHelper() {
    }

    static Object getValue(AnnotatedMember member, Object instance) throws Exception {
        try {
            if (member instanceof AnnotatedField field) {
                return ((Field) field.getMember()).get(instance);
            } else if (member instanceof AnnotatedMethod method) {
                return invoke(method.getAnnotated(), instance);
            }
            return null;
        } catch (InvocationTargetException e) {
            throw ClassUtil.sneakyThrow(e.getCause());
        }
    }

    static Object setValue(AnnotatedMember member, Object instance, Object value) throws Throwable {
        try {
            if (member instanceof AnnotatedMethod method) {
                return method.getAnnotated().invoke(instance, value);
            }
            ((AnnotatedField) member).getAnnotated().set(instance, value);
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static void throwAsJacksonException(JsonParser parser, Throwable cause, Object value, PropertyName propertyName,
            JavaType type) throws JacksonException {
        if (cause instanceof IllegalArgumentException) {
            String actualType = ClassUtil.classNameOf(value);
            StringBuilder message = new StringBuilder("Problem deserializing property '")
                    .append(propertyName.getSimpleName())
                    .append("' (expected type: ")
                    .append(type)
                    .append("; actual type: ")
                    .append(actualType)
                    .append(")");
            String originalMessage = ClassUtil.exceptionMessage(cause);
            if (originalMessage != null) {
                message.append(", problem: ").append(originalMessage);
            } else {
                message.append(" (no error message provided)");
            }
            throw DatabindException.from(parser, message.toString(), cause);
        }
        ClassUtil.throwIfError(cause);
        ClassUtil.throwIfRTE(cause);
        ClassUtil.throwIfJacksonE(cause);
        throw DatabindException.from(parser, ClassUtil.exceptionMessage(cause), cause);
    }

    static Object invoke(Method method, Object receiver, Object... arguments) throws Exception {
        try {
            return method.invoke(receiver, arguments);
        } catch (InvocationTargetException e) {
            throw ClassUtil.sneakyThrow(e.getCause());
        }
    }

    static Object newInstance(Constructor<?> constructor, Object... arguments) throws Exception {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException e) {
            throw ClassUtil.sneakyThrow(e.getCause());
        }
    }
}
