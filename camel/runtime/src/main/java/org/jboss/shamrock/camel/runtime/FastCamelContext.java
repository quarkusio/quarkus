package org.jboss.shamrock.camel.runtime;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Component;
import org.apache.camel.component.headersmap.FastHeadersMapFactory;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.HeadersMapFactory;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.UuidGenerator;

public class FastCamelContext extends DefaultCamelContext {

    public FastCamelContext() {
        super(false);
        setInitialization(Initialization.Eager);
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
    protected UuidGenerator createUuidGenerator() {
        return new FastUuidGenerator();
    }

    @Override
    protected HeadersMapFactory createHeadersMapFactory() {
        return new FastHeadersMapFactory();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        return (name, context) -> resolve(Component.class, "component", name, context);
    }
    @Override
    protected LanguageResolver createLanguageResolver() {
        return (name, context) -> resolve(Language.class, "language", name, context);
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
                return resolve(DataFormat.class, "dataformat", name, context);
            }
        };
    }

    protected <T> T resolve(Class<T> clazz, String type, String name, CamelContext context) {
        T result = context.getRegistry().lookupByNameAndType(name, clazz);
        if (result instanceof CamelContextAware) {
            ((CamelContextAware) result).setCamelContext(context);
        }
        PropertiesComponent comp = getPropertiesComponent();
        if (comp != null) {
            Properties props = comp.getInitialProperties();
            if (props != null) {
                String pfx = CamelRuntime.PFX_CAMEL + type + "." + name;
                log.info("Binding {} {} with prefix {}", type, name, pfx);
                RuntimeSupport.bindProperties(props, result, pfx);
            }
        }
        return (T) result;
    }
}
