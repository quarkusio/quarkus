package io.quarkus.camel.core.runtime.graal;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.camel.Producer;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.converter.jaxp.DomConverter;
import org.apache.camel.converter.jaxp.StaxConverter;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.support.IntrospectionSupport.ClassInfo;
import org.apache.camel.support.LRUCacheFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class CamelSubstitutions {
}

@TargetClass(className = "com.sun.beans.WeakCache")
@Substitute
final class Target_com_sun_beans_WeakCache<K, V> {

    @Substitute
    private Map<K, Reference<V>> map = new WeakHashMap<>();

    @Substitute
    public Target_com_sun_beans_WeakCache() {
    }

    @Substitute
    public V get(K key) {
        Reference<V> reference = this.map.get(key);
        if (reference == null) {
            return null;
        }
        V value = reference.get();
        if (value == null) {
            this.map.remove(key);
        }
        return value;
    }

    @Substitute
    public void put(K key, V value) {
        if (value != null) {
            this.map.put(key, new WeakReference<V>(value));
        } else {
            this.map.remove(key);
        }
    }

    @Substitute
    public void clear() {
        this.map.clear();
    }

}

@TargetClass(className = "java.beans.Introspector")
final class Target_java_beans_Introspector {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Target_com_sun_beans_WeakCache<Class<?>, Method[]> declaredMethodCache = new Target_com_sun_beans_WeakCache<>();

}

@TargetClass(className = "org.apache.camel.support.IntrospectionSupport")
final class Target_org_apache_camel_support_IntrospectionSupport {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, ClassInfo> CACHE = LRUCacheFactory.newLRUWeakCache(256);

}

@TargetClass(className = "org.apache.camel.component.bean.BeanInfo")
final class Target_org_apache_camel_component_bean_BeanInfo {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static List<Method> EXCLUDED_METHODS;

    static {
        EXCLUDED_METHODS = new ArrayList<>();
        // exclude all java.lang.Object methods as we dont want to invoke them
        EXCLUDED_METHODS.addAll(Arrays.asList(Object.class.getMethods()));
        // exclude all java.lang.reflect.Proxy methods as we dont want to invoke them
        EXCLUDED_METHODS.addAll(Arrays.asList(Proxy.class.getMethods()));
        try {
            // but keep toString as this method is okay
            EXCLUDED_METHODS.remove(Object.class.getDeclaredMethod("toString"));
            EXCLUDED_METHODS.remove(Proxy.class.getDeclaredMethod("toString"));
        } catch (Throwable e) {
            // ignore
        }
    }

}

@TargetClass(className = "org.apache.camel.util.HostUtils")
final class Target_org_apache_camel_util_HostUtils {

    @Substitute
    private static InetAddress chooseAddress() throws UnknownHostException {
        return InetAddress.getByName("0.0.0.0");
    }
}

@TargetClass(className = "org.apache.camel.builder.xml.XPathBuilder", onlyWith = XmlDisabled.class)
final class Target_org_apache_camel_builder_xml_XPathBuilder {

    @Substitute
    public static XPathBuilder xpath(String text) {
        throw new UnsupportedOperationException();
    }

    @Substitute
    public static XPathBuilder xpath(String text, Class<?> resultType) {
        throw new UnsupportedOperationException();
    }

}

@TargetClass(className = "org.apache.camel.component.validator.ValidatorEndpoint", onlyWith = XmlDisabled.class)
final class Target_org_apache_camel_component_validator_ValidatorEndpoint {

    @Substitute
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException();
    }
}

@TargetClass(className = "org.apache.camel.impl.converter.CoreStaticTypeConverterLoader", onlyWith = XmlDisabled.class)
final class Target_org_apache_camel_impl_converter_CoreStaticTypeConverterLoader {

    @Substitute
    private XmlConverter getXmlConverter() {
        throw new UnsupportedOperationException();
    }

    @Substitute
    private DomConverter getDomConverter() {
        throw new UnsupportedOperationException();
    }

    @Substitute
    private StaxConverter getStaxConverter() {
        throw new UnsupportedOperationException();
    }
}
