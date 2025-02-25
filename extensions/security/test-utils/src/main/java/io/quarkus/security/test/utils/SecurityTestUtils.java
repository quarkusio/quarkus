package io.quarkus.security.test.utils;

import static io.quarkus.security.test.utils.IdentityMock.setUpAuth;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import io.smallrye.mutiny.Uni;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityTestUtils {
    public static <T> void assertSuccess(Supplier<T> action, T expectedResult, AuthData... auth) {
        for (AuthData authData : auth) {
            setUpAuth(authData);
            Assertions.assertEquals(expectedResult, action.get());
        }
        if (auth.length == 0) {
            throw new IllegalStateException("No tests were executed as AuthData are missing");
        }
    }

    public static <T> void assertSuccess(Uni<T> action, T expectedResult, AuthData authData) {
        setUpAuth(authData);
        action.subscribe().with(new Consumer<T>() {
            @Override
            public void accept(T actual) {
                Assertions.assertEquals(expectedResult, actual);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                Assertions.fail("Assertion failed with: " + throwable.getMessage());
            }
        });
    }

    public static void assertFailureFor(Executable action, Class<? extends Exception> expectedException,
            AuthData... auth) {
        for (AuthData authData : auth) {
            setUpAuth(authData);
            Assertions.assertThrows(expectedException, action);
        }
        if (auth.length == 0) {
            throw new IllegalStateException("No tests were executed as AuthData are missing");
        }
    }

    public static <T> void assertFailureFor(Uni<T> action, Class<? extends Exception> expectedException, AuthData authData) {
        setUpAuth(authData);
        action.subscribe().with(new Consumer<T>() {
            @Override
            public void accept(T actual) {
                Assertions.fail(String.format("Expected exception %s was never thrown", expectedException));
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable actual) {
                Assertions.assertEquals(expectedException, actual.getClass());
            }
        });
    }

    private SecurityTestUtils() {
    }
}
