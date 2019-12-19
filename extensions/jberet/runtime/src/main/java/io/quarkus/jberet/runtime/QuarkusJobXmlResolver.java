package io.quarkus.jberet.runtime;

import java.io.InputStream;
import java.util.Collection;

import org.jberet.spi.JobXmlResolver;

// TODO: Do we need this class at all? Perhaps we could get rid of it.
class QuarkusJobXmlResolver implements JobXmlResolver {

    @Override
    public InputStream resolveJobXml(String s, ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getJobXmlNames(ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveJobName(String s, ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }
}
