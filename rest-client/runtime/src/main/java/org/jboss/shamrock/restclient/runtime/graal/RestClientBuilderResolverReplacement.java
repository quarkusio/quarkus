package org.jboss.shamrock.restclient.runtime.graal;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.shamrock.restclient.runtime.BuilderResolver;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(RestClientBuilderResolver.class)
final class RestClientBuilderResolverReplacement {

    @Substitute
    private static RestClientBuilderResolver loadSpi(ClassLoader cl) {
        return new BuilderResolver();
    }
}
