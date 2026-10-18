package io.quarkus.tck.restclient.invalid;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.tck.interfaces.MissingUriTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class InterfaceHasMethodWithPathParamAnnotationButNoURITemplateTest {
    @Deployment
    @ShouldThrowException(RestClientDefinitionException.class)
    public static Archive<?> createDeployment() {
        return ShrinkWrap
                .create(WebArchive.class,
                        InterfaceHasMethodWithPathParamAnnotationButNoURITemplateTest.class.getSimpleName()
                                + ".war")
                .addClasses(MissingUriTemplate.class);
    }

    @Test
    public void shouldNotBeInvoked() {

    }
}
