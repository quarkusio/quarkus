package io.quarkus.security.test.cdi;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnFinalMethodWithDisableFinalRemovalTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanWithSecuredFinalMethod.class, IdentityMock.class,
                            AuthData.class, SecurityTestUtils.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.remove-final-for-proxyable-methods=false"),
                            "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Inject
    BeanWithSecuredFinalMethod bean;

    @Test
    public void test() {
        // should never be executed since the application should not be built
        fail();
    }

}
