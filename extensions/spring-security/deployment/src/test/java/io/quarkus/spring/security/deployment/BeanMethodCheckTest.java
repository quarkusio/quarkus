package io.quarkus.spring.security.deployment;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.spring.security.deployment.springapp.BeanWithBeanMethodChecks;
import io.quarkus.spring.security.deployment.springapp.Person;
import io.quarkus.spring.security.deployment.springapp.PersonChecker;
import io.quarkus.spring.security.deployment.springapp.PersonCheckerImpl;
import io.quarkus.spring.security.deployment.springapp.PrincipalChecker;
import io.quarkus.spring.security.deployment.springapp.SomeInterface;
import io.quarkus.spring.security.deployment.springapp.SomeInterfaceImpl;
import io.quarkus.spring.security.deployment.springapp.SpringConfiguration;
import io.quarkus.test.QuarkusUnitTest;

public class BeanMethodCheckTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(
                            Person.class,
                            PersonChecker.class,
                            PersonCheckerImpl.class,
                            PrincipalChecker.class,
                            BeanWithBeanMethodChecks.class,
                            SomeInterface.class,
                            SomeInterfaceImpl.class,
                            SpringConfiguration.class,
                            IdentityMock.class,
                            AuthData.class,
                            SecurityTestUtils.class));

    @Inject
    BeanWithBeanMethodChecks beanWithBeanMethodChecks;

    @Inject
    SomeInterface someInterface;

    @Test
    public void testNoParamsAlwaysPasses() {
        assertSuccess(() -> beanWithBeanMethodChecks.noParamsAlwaysPasses(), "noParamsAlwaysPasses", ANONYMOUS);
        assertSuccess(() -> beanWithBeanMethodChecks.noParamsAlwaysPasses(), "noParamsAlwaysPasses", ADMIN);
        assertSuccess(() -> beanWithBeanMethodChecks.noParamsAlwaysPasses(), "noParamsAlwaysPasses", USER);
    }

    @Test
    public void testNoParamsNeverPasses() {
        assertFailureFor(() -> beanWithBeanMethodChecks.noParamsNeverPasses(), UnauthorizedException.class, ANONYMOUS);
        assertFailureFor(() -> beanWithBeanMethodChecks.noParamsNeverPasses(), ForbiddenException.class, USER);
        assertFailureFor(() -> beanWithBeanMethodChecks.noParamsNeverPasses(), ForbiddenException.class, ADMIN);
    }

    @Test
    public void testWithParams() {
        assertFailureFor(() -> beanWithBeanMethodChecks.withParams("other", new Person("geo")), UnauthorizedException.class,
                ANONYMOUS);
        assertSuccess(() -> beanWithBeanMethodChecks.withParams("geo", new Person("geo")), "withParams", ANONYMOUS);
        assertFailureFor(() -> beanWithBeanMethodChecks.withParams("other", new Person("geo")), ForbiddenException.class, USER);
        assertSuccess(() -> beanWithBeanMethodChecks.withParams("geo", new Person("geo")), "withParams", USER);
    }

    // the reason for having this test is to ensure that caching of the generated classes doesn't mess up anything
    @Test
    public void testAnotherWithParams() {
        assertFailureFor(() -> beanWithBeanMethodChecks.withParams("other", new Person("geo")), UnauthorizedException.class,
                ANONYMOUS);
        assertSuccess(() -> beanWithBeanMethodChecks.withParams("geo", new Person("geo")), "withParams", ANONYMOUS);
        assertFailureFor(() -> beanWithBeanMethodChecks.withParams("other", new Person("geo")), ForbiddenException.class, USER);
        assertSuccess(() -> beanWithBeanMethodChecks.withParams("geo", new Person("geo")), "withParams", USER);
    }

    @Test
    public void testWithParamsAndConstant() {
        assertSuccess(() -> beanWithBeanMethodChecks.withParamAndConstant(new Person("geo")), "withParamAndConstant",
                ANONYMOUS);
        assertFailureFor(() -> beanWithBeanMethodChecks.withParamAndConstant(new Person("other")), ForbiddenException.class,
                USER);
        assertSuccess(() -> beanWithBeanMethodChecks.withParamAndConstant(new Person("geo")), "withParamAndConstant", USER);
    }

    @Test
    public void testWithExtraUnusedParam() {
        assertFailureFor(() -> someInterface.doSomething("other", 1, new Person("geo")), UnauthorizedException.class,
                ANONYMOUS);
        assertSuccess(() -> someInterface.doSomething("geo", 1, new Person("geo")), "doSomething", ANONYMOUS);
        assertFailureFor(() -> someInterface.doSomething("other", -1, new Person("geo")), ForbiddenException.class, USER);
        assertSuccess(() -> someInterface.doSomething("geo", -1, new Person("geo")), "doSomething", USER);
    }

    @Test
    public void testPrincipalUsername() {
        assertFailureFor(() -> beanWithBeanMethodChecks.principalChecker("user"), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(() -> beanWithBeanMethodChecks.principalChecker("other"), ForbiddenException.class, USER);
        assertSuccess(() -> beanWithBeanMethodChecks.principalChecker("user"), "principalChecker", USER);
    }
}
