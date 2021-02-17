package io.quarkus.tck.restclient.invalid;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.tck.interfaces.MultipleHeadersOnSameMethod;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class MultipleClientHeaderParamsSpecifySameHeaderOnMethodTest {
    @Deployment
    @ShouldThrowException(RestClientDefinitionException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class,
                        MultipleClientHeaderParamsSpecifySameHeaderOnMethodTest.class.getSimpleName() + ".war")
                .addClasses(MultipleHeadersOnSameMethod.class);
    }

    @Test
    public void shouldNotBeInvoked() {

    }
}
