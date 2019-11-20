package io.quarkus.security.test.cdi.ext;

import static io.quarkus.security.test.cdi.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.cdi.SecurityTestUtils;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.test.QuarkusUnitTest;

public class CDIAccessGeneratedBeanTest {

    @Inject
    GeneratedBean generatedBean;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(IdentityMock.class,
                            SecurityTestUtils.class,
                            GeneratedBean.class,
                            GenereateBeanBuildStep.class));

    @Test
    public void shouldFailToAccessForbidden() {
        assertFailureFor(() -> generatedBean.secured(), UnauthorizedException.class, ANONYMOUS);

    }

}
