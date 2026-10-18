package org.acme;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.xml.bind.JAXBContext;

public class Producer {

    @Singleton
    @Produces
    protected JAXBContext createJAXBContext() throws Exception {
        return JAXBContext.newInstance(); // This line causes the LinkageError
    }
}
