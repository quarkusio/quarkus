package org.jboss.resteasy.reactive.server.processor.reflection;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.STRING;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.EndpointIndexer;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectionConstructorParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.reflection.ReflectionValueOfParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.processor.ServerEndpointIndexer;

public class ReflectionConverterIndexerExtension implements ServerEndpointIndexer.ConverterSupplierIndexerExtension {

    @Override
    public ParameterConverterSupplier extractConverterImpl(String elementType, IndexView indexView,
                                                           Map<String, String> existingConverters, String errorLocation, boolean hasRuntimeConverters) {
        MethodInfo fromString = null;
        MethodInfo valueOf = null;
        MethodInfo stringCtor = null;
        String primitiveWrapperType = EndpointIndexer.primitiveTypes.get(elementType);
        String prefix = "";
        if (primitiveWrapperType != null) {
            return new ReflectionValueOfParameterConverterSupplier(primitiveWrapperType);
        } else {
            ClassInfo type = indexView.getClassByName(DotName.createSimple(elementType));
            if (type != null) {
                for (MethodInfo i : type.methods()) {
                    boolean isStatic = ((i.flags() & Modifier.STATIC) != 0);
                    boolean isNotPrivate = (i.flags() & Modifier.PRIVATE) == 0;
                    if ((i.parametersCount() == 1) && isNotPrivate) {
                        if (i.parameterType(0).name().equals(STRING)) {
                            if (i.name().equals("<init>")) {
                                stringCtor = i;
                            } else if (i.name().equals("valueOf") && isStatic) {
                                valueOf = i;
                            } else if (i.name().equals("fromString") && isStatic) {
                                fromString = i;
                            }
                        }
                    }
                }
                if (type.isEnum()) {
                    //spec weirdness, enums order is different
                    if (fromString != null) {
                        valueOf = null;
                    }
                }
            }
        }
        ParameterConverterSupplier delegate = null;
        if (stringCtor != null) {
            delegate = new ReflectionConstructorParameterConverterSupplier(stringCtor.declaringClass().name().toString());
        } else if (valueOf != null) {
            delegate = new ReflectionValueOfParameterConverterSupplier(valueOf.declaringClass().name().toString());
        } else if (fromString != null) {
            delegate = new ReflectionValueOfParameterConverterSupplier(fromString.declaringClass().name().toString(),
                    "fromString");
        }
        if (hasRuntimeConverters)
            return new RuntimeResolvedConverter.Supplier().setDelegate(delegate);
        if (delegate == null)
            throw new RuntimeException("Failed to find converter for " + elementType);
        return delegate;
    }
}
