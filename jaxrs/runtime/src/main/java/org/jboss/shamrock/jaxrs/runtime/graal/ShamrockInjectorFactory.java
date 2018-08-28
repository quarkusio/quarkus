package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.reflect.Constructor;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.shamrock.runtime.BeanContainer;

/**
 * Created by bob on 7/31/18.
 */
public class ShamrockInjectorFactory extends InjectorFactoryImpl {

    public static volatile BeanContainer CONTAINER = null;

    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory providerFactory) {
        System.err.println( "create constructor: " + constructor );
        return super.createConstructor(constructor, providerFactory);
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        System.err.println( "create resource constructor: " + constructor.getConstructor() );
        return new ShamrockConstructorInjector(constructor.getConstructor(), super.createConstructor(constructor, providerFactory));
    }
}
