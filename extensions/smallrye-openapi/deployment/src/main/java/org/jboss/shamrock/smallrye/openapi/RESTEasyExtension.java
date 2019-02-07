package org.jboss.shamrock.smallrye.openapi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.shamrock.deployment.util.ServiceUtil;
import org.jboss.shamrock.jaxrs.JaxrsConfig;

import io.smallrye.openapi.api.OpenApiConstants;
import io.smallrye.openapi.runtime.scanner.DefaultAnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import io.smallrye.openapi.runtime.util.JandexUtil;
import io.smallrye.openapi.runtime.util.JandexUtil.JaxRsParameterInfo;

public class RESTEasyExtension extends DefaultAnnotationScannerExtension {

    public static final DotName DOTNAME_QUERY_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.QueryParam");
    public static final DotName DOTNAME_FORM_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.FormParam");
    public static final DotName DOTNAME_COOKIE_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.CookieParam");
    public static final DotName DOTNAME_PATH_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.PathParam");
    public static final DotName DOTNAME_HEADER_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.HeaderParam");
    public static final DotName DOTNAME_MATRIX_PARAM = DotName.createSimple("org.jboss.resteasy.annotations.jaxrs.MatrixParam");
    private static final DotName DOTNAME_PROVIDER = DotName.createSimple("javax.ws.rs.ext.Provider");
    private static final DotName DOTNAME_ASYNC_RESPONSE_PROVIDER = DotName.createSimple("org.jboss.resteasy.spi.AsyncResponseProvider");

    private List<DotName> asyncTypes = new ArrayList<>();
    private String defaultPath;

    public RESTEasyExtension(JaxrsConfig jaxrsConfig, IndexView index) {
        this.defaultPath = jaxrsConfig.defaultPath;
        // the index is not enough to scan for providers because it does not contain
        // dependencies, so we have to rely on scanning the declared providers via services
        scanAsyncResponseProvidersFromServices();
        scanAsyncResponseProviders(index);
    }

    private void scanAsyncResponseProvidersFromServices() {
        try {
            Class<?> asyncResponseProvider = Class.forName("org.jboss.resteasy.spi.AsyncResponseProvider");
            // can't use the ServiceLoader API because Providers is not an interface
            for (String provider : ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                    "META-INF/services/javax.ws.rs.ext.Providers")) {
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
            Class<?> klass = Class.forName(name);
            if(asyncResponseProviderClass.isAssignableFrom(klass)) {
                for (java.lang.reflect.Type type : klass.getGenericInterfaces()) {
                    if(type instanceof java.lang.reflect.ParameterizedType) {
                        java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) type;
                        if(pType.getRawType().equals(asyncResponseProviderClass)
                                && pType.getActualTypeArguments().length == 1) {
                            java.lang.reflect.Type asyncType = pType.getActualTypeArguments()[0];
                            String asyncTypeName;
                            if(asyncType instanceof java.lang.reflect.ParameterizedType)
                                asyncTypeName = ((java.lang.reflect.ParameterizedType) asyncType).getRawType().getTypeName();
                            else
                                asyncTypeName = asyncType.getTypeName();
                            asyncTypes.add(DotName.createSimple(asyncTypeName));
                        }
                    }
                }
            }
        }catch(ClassNotFoundException x) {
            // ignore it
        }
    }

    private void scanAsyncResponseProviders(IndexView index) {
        for (ClassInfo providerClass : index.getAllKnownImplementors(DOTNAME_ASYNC_RESPONSE_PROVIDER)) {
            for (AnnotationInstance annotation : providerClass.classAnnotations()) {
                if(annotation.name().equals(DOTNAME_PROVIDER)){
                    for (Type interf : providerClass.interfaceTypes()) {
                        if(interf.kind() == Type.Kind.PARAMETERIZED_TYPE
                                && interf.name().equals(DOTNAME_ASYNC_RESPONSE_PROVIDER)) {
                            ParameterizedType pType = interf.asParameterizedType();
                            if(pType.arguments().size() == 1) {
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
    public JaxRsParameterInfo getMethodParameterJaxRsInfo(MethodInfo method, int idx) {
        AnnotationInstance jaxRsAnno = JandexUtil.getMethodParameterAnnotation(method, idx, DOTNAME_PATH_PARAM);
        if (jaxRsAnno != null) {
            JaxRsParameterInfo info = new JaxRsParameterInfo();
            info.in = In.PATH;
            info.name = JandexUtil.stringValue(jaxRsAnno, OpenApiConstants.PROP_VALUE);
            if(info.name == null)
                info.name = method.parameterName(idx);
            return info;
        }

        jaxRsAnno = JandexUtil.getMethodParameterAnnotation(method, idx, DOTNAME_QUERY_PARAM);
        if (jaxRsAnno != null) {
            JaxRsParameterInfo info = new JaxRsParameterInfo();
            info.in = In.QUERY;
            info.name = JandexUtil.stringValue(jaxRsAnno, OpenApiConstants.PROP_VALUE);
            if(info.name == null)
                info.name = method.parameterName(idx);
            return info;
        }

        jaxRsAnno = JandexUtil.getMethodParameterAnnotation(method, idx, DOTNAME_COOKIE_PARAM);
        if (jaxRsAnno != null) {
            JaxRsParameterInfo info = new JaxRsParameterInfo();
            info.in = In.COOKIE;
            info.name = JandexUtil.stringValue(jaxRsAnno, OpenApiConstants.PROP_VALUE);
            if(info.name == null)
                info.name = method.parameterName(idx);
            return info;
        }

        jaxRsAnno = JandexUtil.getMethodParameterAnnotation(method, idx, DOTNAME_HEADER_PARAM);
        if (jaxRsAnno != null) {
            JaxRsParameterInfo info = new JaxRsParameterInfo();
            info.in = In.HEADER;
            info.name = JandexUtil.stringValue(jaxRsAnno, OpenApiConstants.PROP_VALUE);
            if(info.name == null)
                info.name = method.parameterName(idx);
            return info;
        }

        return null;
    }

    @Override
    public In parameterIn(MethodParameterInfo paramInfo) {
        MethodInfo method = paramInfo.method();
        short paramPosition = paramInfo.position();
        List<AnnotationInstance> annotations = JandexUtil.getParameterAnnotations(method, paramPosition);
        for (AnnotationInstance annotation : annotations) {
            if (annotation.name().equals(DOTNAME_QUERY_PARAM)) {
                return In.QUERY;
            }
            if (annotation.name().equals(DOTNAME_PATH_PARAM)) {
                return In.PATH;
            }
            if (annotation.name().equals(DOTNAME_HEADER_PARAM)) {
                return In.HEADER;
            }
            if (annotation.name().equals(DOTNAME_COOKIE_PARAM)) {
                return In.COOKIE;
            }
        }
        return null;
    }

    @Override
    public Type resolveAsyncType(Type type) {
        if(type.kind() == Type.Kind.PARAMETERIZED_TYPE
                && asyncTypes.contains(type.name())) {
            ParameterizedType pType = type.asParameterizedType();
            if(pType.arguments().size() == 1)
                return pType.arguments().get(0);
        }
        return super.resolveAsyncType(type);
    }

    @Override
    public void processJaxRsApplications(OpenApiAnnotationScanner scanner, Collection<ClassInfo> applications) {
        if(applications.isEmpty())
            scanner.setCurrentAppPath(defaultPath);
    }
}
