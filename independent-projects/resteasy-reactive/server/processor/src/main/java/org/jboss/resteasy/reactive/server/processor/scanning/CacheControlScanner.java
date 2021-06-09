package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.NoCache;
import org.jboss.resteasy.reactive.common.util.ExtendedCacheControl;
import org.jboss.resteasy.reactive.server.handlers.CacheControlHandler;
import org.jboss.resteasy.reactive.server.model.FixedHandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;

public class CacheControlScanner implements MethodScanner {

    private static final DotName NO_CACHE = DotName.createSimple(NoCache.class.getName());
    private static final DotName CACHE = DotName.createSimple(Cache.class.getName());

    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
            Map<String, Object> methodContext) {
        ExtendedCacheControl cacheControl = noCacheToCacheControl(method.annotation(NO_CACHE));
        if (cacheControl != null) {
            if (method.annotation(CACHE) != null) {
                throw new IllegalStateException(
                        "A resource method cannot be simultaneously annotated with '@Cache' and '@NoCache'. Offending method is '"
                                + method.name() + "' of class '" + method.declaringClass().name() + "'");
            }
            return cacheControlToCustomizerList(cacheControl);
        } else {
            cacheControl = noCacheToCacheControl(actualEndpointClass.classAnnotation(NO_CACHE));
            if (cacheControl != null) {
                if (actualEndpointClass.classAnnotation(CACHE) != null) {
                    throw new IllegalStateException(
                            "A resource class cannot be simultaneously annotated with '@Cache' and '@NoCache'. Offending class is '"
                                    + actualEndpointClass.name() + "'");
                }
                return cacheControlToCustomizerList(cacheControl);
            }
        }

        cacheControl = cacheToCacheControl(method.annotation(CACHE));
        if (cacheControl != null) {
            return cacheControlToCustomizerList(cacheControl);
        } else {
            cacheControl = cacheToCacheControl(actualEndpointClass.classAnnotation(CACHE));
            if (cacheControl != null) {
                return cacheControlToCustomizerList(cacheControl);
            }
        }

        return Collections.emptyList();
    }

    private ExtendedCacheControl noCacheToCacheControl(AnnotationInstance noCacheInstance) {
        if (noCacheInstance == null) {
            return null;
        }
        AnnotationValue fieldsValue = noCacheInstance.value("fields");
        if (fieldsValue != null) {
            String[] fields = fieldsValue.asStringArray();
            if ((fields != null) && (fields.length > 0)) {
                ExtendedCacheControl cacheControl = new ExtendedCacheControl();
                cacheControl.setNoCache(true);
                cacheControl.setNoTransform(false);
                cacheControl.getNoCacheFields().addAll(Arrays.asList(fields));
                return cacheControl;
            }
        }
        return null;
    }

    private ExtendedCacheControl cacheToCacheControl(AnnotationInstance cacheInstance) {
        if (cacheInstance == null) {
            return null;
        }
        ExtendedCacheControl cacheControl = new ExtendedCacheControl();
        AnnotationValue maxAgeValue = cacheInstance.value("maxAge");
        if (maxAgeValue != null) {
            int maxAge = maxAgeValue.asInt();
            if (maxAge > -1) {
                cacheControl.setMaxAge(maxAge);
            }
        }
        AnnotationValue sMaxAgeValue = cacheInstance.value("sMaxAge");
        if (sMaxAgeValue != null) {
            int sMaxAge = sMaxAgeValue.asInt();
            if (sMaxAge > -1) {
                cacheControl.setSMaxAge(sMaxAge);
            }
        }
        AnnotationValue noStoreValue = cacheInstance.value("noStore");
        if (noStoreValue != null) {
            cacheControl.setNoStore(noStoreValue.asBoolean());
        }
        AnnotationValue noTransformValue = cacheInstance.value("noTransform");
        if (noTransformValue != null) {
            cacheControl.setNoTransform(noTransformValue.asBoolean());
        } else {
            cacheControl.setNoTransform(false); // for some reason the CacheControl class sets the default to true...
        }
        AnnotationValue mustRevalidateValue = cacheInstance.value("mustRevalidate");
        if (mustRevalidateValue != null) {
            cacheControl.setMustRevalidate(mustRevalidateValue.asBoolean());
        }
        AnnotationValue proxyRevalidateValue = cacheInstance.value("proxyRevalidate");
        if (proxyRevalidateValue != null) {
            cacheControl.setProxyRevalidate(proxyRevalidateValue.asBoolean());
        }
        AnnotationValue isPrivateValue = cacheInstance.value("isPrivate");
        if (isPrivateValue != null) {
            cacheControl.setPrivate(isPrivateValue.asBoolean());
        }
        AnnotationValue noCacheValue = cacheInstance.value("noCache");
        if (noCacheValue != null) {
            cacheControl.setNoCache(noCacheValue.asBoolean());
        }
        return cacheControl;
    }

    private List<HandlerChainCustomizer> cacheControlToCustomizerList(ExtendedCacheControl cacheControl) {
        CacheControlHandler handler = new CacheControlHandler();
        handler.setCacheControl(cacheControl);
        return Collections.singletonList(new FixedHandlerChainCustomizer(handler,
                HandlerChainCustomizer.Phase.AFTER_RESPONSE_CREATED));
    }
}
