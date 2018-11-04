package org.jboss.shamrock.camel.runtime;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.UuidGenerator;
import org.jboss.shamrock.runtime.InjectionInstance;

public class FastCamelContext extends DefaultCamelContext {

    public FastCamelContext() {
        super(false);
    }

    @Override
    protected Registry createRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ManagementNameStrategy createManagementNameStrategy() {
        return null;
    }

    @Override
    protected ShutdownStrategy createShutdownStrategy() {
        return new NoShutdownStrategy();
    }

    @Override
    protected UuidGenerator createDefaultUuidGenerator() {
        return new FastUuidGenerator();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        return (name, context) -> resolve(Component.class, name, context);
    }
    @Override
    protected LanguageResolver createLanguageResolver() {
        return (name, context) -> resolve(Language.class, name, context);
    }
    @Override
    protected DataFormatResolver createDataFormatResolver() {
        return new DataFormatResolver() {
            @Override
            public DataFormat resolveDataFormat(String name, CamelContext context) {
                return createDataFormat(name, context);
            }
            @Override
            public DataFormat createDataFormat(String name, CamelContext context) {
                return resolve(DataFormat.class, name, context);
            }
        };
    }

    protected <T> T resolve(Class<T> type, String name, CamelContext context) {
        Object result = context.getRegistry().lookupByName(name);
        if (result instanceof InjectionInstance) {
            log.info("Instantiating " + type.getName());
            result = ((InjectionInstance) result).newInstance();
        }
        if (type.isInstance(result)) {
            if (result instanceof CamelContextAware) {
                ((CamelContextAware) result).setCamelContext(context);
            }
            return (T) result;
        } else {
            throw new IllegalArgumentException("Unable to resolve " + type.getName() + " named " + name);
        }
    }
}
