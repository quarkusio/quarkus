package io.quarkus.hibernate.orm.runtime.customized;

import static org.hibernate.internal.CoreLogging.messageLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;
import org.hibernate.type.CompositeType;

import io.quarkus.hibernate.orm.runtime.proxies.ProxyDefinitions;

/**
 * This {@link ProxyFactory} implementation is very similar to the {@link ByteBuddyProxyFactory},
 * except the class definitions of the proxies have been built upfront so to allow
 * usage of enhanced proxies in native images.
 */
public final class QuarkusProxyFactory implements ProxyFactory {

    private static final CoreMessageLogger LOG = messageLogger(QuarkusProxyFactory.class);

    private final ProxyDefinitions proxyClassDefinitions;

    private Class<?> persistentClass;
    private String entityName;
    private Class<?>[] interfaces;
    private Method getIdentifierMethod;
    private Method setIdentifierMethod;
    private CompositeType componentIdType;

    // Following have been computed upfront during Augmentation:
    private boolean overridesEquals;
    private Constructor constructor;

    public QuarkusProxyFactory(ProxyDefinitions proxyClassDefinitions) {
        this.proxyClassDefinitions = proxyClassDefinitions;
    }

    @Override
    public void postInstantiate(String entityName, Class<?> persistentClass, Set<Class<?>> interfaces,
            Method getIdentifierMethod,
            Method setIdentifierMethod, CompositeType componentIdType) throws HibernateException {
        this.entityName = entityName;
        this.persistentClass = persistentClass;
        this.interfaces = toArray(interfaces);
        this.getIdentifierMethod = getIdentifierMethod;
        this.setIdentifierMethod = setIdentifierMethod;
        this.componentIdType = componentIdType;
        ProxyDefinitions.ProxyClassDetailsHolder detailsHolder = proxyClassDefinitions.getProxyForClass(persistentClass);
        if (detailsHolder == null) {
            throw new HibernateException("Could not lookup a pre-generated proxy class definition for entity '" + entityName
                    + "'. Falling back to enforced eager mode for this entity!");
        }
        this.overridesEquals = detailsHolder.isOverridesEquals();
        this.constructor = detailsHolder.getConstructor();

    }

    private static Class<?>[] toArray(Set<Class<?>> interfaces) {
        if (interfaces == null) {
            return ArrayHelper.EMPTY_CLASS_ARRAY;
        }

        return interfaces.toArray(new Class[interfaces.size()]);
    }

    @Override
    public HibernateProxy getProxy(
            Object id,
            SharedSessionContractImplementor session) throws HibernateException {
        final ByteBuddyInterceptor interceptor = new ByteBuddyInterceptor(
                entityName,
                persistentClass,
                interfaces,
                id,
                getIdentifierMethod,
                setIdentifierMethod,
                componentIdType,
                session,
                overridesEquals);

        try {
            final HibernateProxy proxy = (HibernateProxy) constructor.newInstance();
            ((ProxyConfiguration) proxy).$$_hibernate_set_interceptor(interceptor);
            return proxy;
        } catch (Throwable t) {
            String logMessage = "Bytecode enhancement failed for class '" + entityName
                    + "' (it might be due to the Java module system preventing Hibernate ORM from defining an enhanced class in the same package"
                    + " - in this case, the class should be opened and exported to Hibernate ORM)";
            LOG.error(logMessage, t);
            throw new HibernateException(logMessage, t);
        }
    }
}
