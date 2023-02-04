package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;

import io.quarkus.deployment.util.ServiceUtil;
import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;

public class RESTEasyExtension implements AnnotationScannerExtension {

    private static final DotName DOTNAME_PROVIDER = DotName.createSimple("jakarta.ws.rs.ext.Provider");
    private static final DotName DOTNAME_ASYNC_RESPONSE_PROVIDER = DotName
            .createSimple("org.jboss.resteasy.spi.AsyncResponseProvider");

    private final List<DotName> asyncTypes = new ArrayList<>();

    public RESTEasyExtension(IndexView index) {
        // the index is not enough to scan for providers because it does not contain
        // dependencies, so we have to rely on scanning the declared providers via services
        scanAsyncResponseProvidersFromServices();
        scanAsyncResponseProviders(index);
    }

    private void scanAsyncResponseProvidersFromServices() {
        try {
            Class<?> asyncResponseProvider = Class.forName("org.jboss.resteasy.spi.AsyncResponseProvider", false,
                    Thread.currentThread().getContextClassLoader());
            // can't use the ServiceLoader API because Providers is not an interface
            for (String provider : ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                    "META-INF/services/jakarta.ws.rs.ext.Providers")) {
                scanAsyncResponseProvidersFromClassName(asyncResponseProvider, provider);
            }
        } catch (IOException e) {
            // failed to index, never mind
        } catch (ClassNotFoundException e) {
            // cannot find the right RESTEasy SPI interface, we can't scan
        }
    }

    private void scanAsyncResponseProvidersFromClassName(Class<?> asyncResponseProviderClass, String name) {
        try {
            Class<?> klass = Class.forName(name, false,
                    Thread.currentThread().getContextClassLoader());
            if (asyncResponseProviderClass.isAssignableFrom(klass)) {
                for (java.lang.reflect.Type type : klass.getGenericInterfaces()) {
                    if (type instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) type;
                        if (pType.getRawType().equals(asyncResponseProviderClass)
                                && pType.getActualTypeArguments().length == 1) {
                            java.lang.reflect.Type asyncType = pType.getActualTypeArguments()[0];
                            String asyncTypeName;
                            if (asyncType instanceof java.lang.reflect.ParameterizedType)
                                asyncTypeName = ((java.lang.reflect.ParameterizedType) asyncType).getRawType().getTypeName();
                            else
                                asyncTypeName = asyncType.getTypeName();
                            asyncTypes.add(DotName.createSimple(asyncTypeName));
                        }
                    }
                }
            }
        } catch (ClassNotFoundException x) {
            // ignore it
        }
    }

    private void scanAsyncResponseProviders(IndexView index) {
        for (ClassInfo providerClass : index.getAllKnownImplementors(DOTNAME_ASYNC_RESPONSE_PROVIDER)) {
            for (AnnotationInstance annotation : providerClass.classAnnotations()) {
                if (annotation.name().equals(DOTNAME_PROVIDER)) {
                    for (Type interf : providerClass.interfaceTypes()) {
                        if (interf.kind() == Type.Kind.PARAMETERIZED_TYPE
                                && interf.name().equals(DOTNAME_ASYNC_RESPONSE_PROVIDER)) {
                            ParameterizedType pType = interf.asParameterizedType();
                            if (pType.arguments().size() == 1) {
                                Type asyncType = pType.arguments().get(0);
                                asyncTypes.add(asyncType.name());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Type resolveAsyncType(Type type) {
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && asyncTypes.contains(type.name())) {
            ParameterizedType pType = type.asParameterizedType();
            if (pType.arguments().size() == 1) {
                return pType.arguments().get(0);
            }
        }
        return null;
    }
}
