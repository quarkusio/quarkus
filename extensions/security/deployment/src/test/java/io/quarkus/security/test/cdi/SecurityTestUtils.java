package io.quarkus.security.test.cdi;

import static io.quarkus.security.test.utils.IdentityMock.setUpAuth;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import io.quarkus.security.test.utils.AuthData;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityTestUtils {
    public static <T> void assertSuccess(Supplier<T> action, T expectedResult, AuthData... auth) {
        for (AuthData authData : auth) {
            setUpAuth(authData);
            Assertions.assertEquals(action.get(), expectedResult);
        }

    }

    public static void assertFailureFor(Executable action, Class<? extends Exception> expectedException,
            AuthData... auth) {
        for (AuthData authData : auth) {
            setUpAuth(authData);
            Assertions.assertThrows(expectedException, action);
        }
    }

    private SecurityTestUtils() {
    }
}
