package io.quarkus.cxf.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractCxfWebServiceProducer {

    private final static Logger LOG = LoggerFactory.getLogger(AbstractCxfWebServiceProducer.class);

    public Object createWebService(String className) {
        try {
            return Class.forName(className).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOG.error("cannot create web service " + className, e);
        }
        return null;
    }
}
