package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

public class TypesafeGraphQLRecursionTest {
    private static String EXPECTED_THROWN_MESSAGE = "SRGQLDC035008: Field recursion found";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RecursiveApi.class, Team.class, Hero.class))
            .assertException(t -> {
                assertEquals(IllegalStateException.class, t.getClass());
                assertTrue(t.getMessage().equals(EXPECTED_THROWN_MESSAGE),
                        "Wrong thrown error message.\nExpected:" + EXPECTED_THROWN_MESSAGE + "\nActual:" +
                                t.getMessage());

            });

    private static class Hero {
        String name;
        List<Team> teams;
    }

    private static class Team {
        String name;
        List<Hero> heroes;
    }

    @GraphQLClientApi
    private interface RecursiveApi {
        Hero member();
    }

    @Test
    void minimalTest() {
        assertFalse(true);
    }
}
