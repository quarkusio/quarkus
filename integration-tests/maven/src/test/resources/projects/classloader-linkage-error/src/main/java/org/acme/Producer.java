package org.acme;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;

public class Producer {

    @Singleton
    @Produces
    protected JAXBContext createJAXBContext() throws Exception {
        return JAXBContext.newInstance(); // This line causes the LinkageError
    }
}
