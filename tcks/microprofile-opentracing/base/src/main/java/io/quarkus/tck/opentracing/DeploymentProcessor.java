package io.quarkus.tck.opentracing;

import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Pavol Loffay
 * @author Jan Martiska
 */
public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            WebArchive war = WebArchive.class.cast(archive);

            // enable tracing on the client side
            war.addAsServiceProvider(ClientTracingRegistrarProvider.class,
                    ResteasyClientTracingRegistrarProvider.class);
            war.addClasses(ResteasyClientTracingRegistrarProvider.class);

            // override the default TracerProducer
            war.addClass(MockTracerProducer.class);

            // workaround for RESTEASY-1758
            war.addClass(ExceptionMapper.class);
        }

    }
}
