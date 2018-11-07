package org.jboss.shamrock.camel.runtime;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultModelJAXBContextFactory;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.UuidGenerator;

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

    private static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = new DefaultModelJAXBContextFactory().newJAXBContext();
        } catch (JAXBException e) {
            throw new RuntimeException("Error creating JAXB Context: " + e.getClass() + ": " + e.getMessage(), e);
        }
    }

    @Override
    protected ModelJAXBContextFactory createModelJAXBContextFactory() {
        return () -> JAXB_CONTEXT;
    }

    protected <T> T resolve(Class<T> type, String name, CamelContext context) {
        T result = context.getRegistry().lookupByNameAndType(name, type);
        if (result instanceof CamelContextAware) {
            ((CamelContextAware) result).setCamelContext(context);
        }
        return (T) result;
    }
}
