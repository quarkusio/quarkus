package io.quarkus.smallrye.openapi.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class RestJsClientUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "'/greeting'             , 'Greeting'",
            "'/greeting/{name}'      , 'GreetingName'",
            "'/api/tasks'            , 'ApiTasks'",
            "'/api/tasks/{id}'       , 'ApiTasksId'",
            "'/users-list'           , 'UsersList'",
            "'/user_profile'         , 'UserProfile'",
            "'/v1.0/items'           , 'V10Items'",
            "'/'                     , ''",
            "'/a'                    , 'A'",
            "'/a/b/c'               , 'ABC'",
            "'/api/{org}/{repo}'     , 'ApiOrgRepo'",
    })
    void sanitizePath(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.sanitizePath(input));
    }

    @ParameterizedTest
    @CsvSource({
            "'GreetingResource' , 'GreetingResource'",
            "'Default'          , 'Default'",
            "'my-tag'           , 'mytag'",
            "'my tag'           , 'mytag'",
            "'123invalid'       , 'invalid'",
            "'_private'         , '_private'",
            "'$special'         , '$special'",
            "'validName123'     , 'validName123'",
    })
    void sanitizeIdentifier(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.sanitizeIdentifier(input));
    }

    @ParameterizedTest
    @CsvSource({
            "''          , 'Default'",
            "'!!!'       , 'Default'",
    })
    void sanitizeIdentifierFallsBackToDefault(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.sanitizeIdentifier(input));
    }

    @ParameterizedTest
    @CsvSource({
            "'getGreeting'     , 'getGreeting'",
            "'hello_world'     , 'hello_world'",
            "'my-method'       , 'mymethod'",
            "'_private'        , '_private'",
            "'$special'        , '$special'",
            "'valid123'        , 'valid123'",
    })
    void escapeJsIdentifier(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.escapeJsIdentifier(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void escapeJsIdentifierFallsBackToUnnamed(String input) {
        assertEquals("unnamed", SmallRyeOpenApiProcessor.escapeJsIdentifier(input));
    }

    @ParameterizedTest
    @CsvSource({
            "'!!!'       , 'unnamed'",
    })
    void escapeJsIdentifierInvalidChars(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.escapeJsIdentifier(input));
    }

    @ParameterizedTest
    @MethodSource("escapeJsArgs")
    void escapeJs(String input, String expected) {
        assertEquals(expected, SmallRyeOpenApiProcessor.escapeJs(input));
    }

    static Stream<Arguments> escapeJsArgs() {
        return Stream.of(
                Arguments.of("hello", "hello"),
                Arguments.of("it's", "it\\'s"),
                Arguments.of("back\\slash", "back\\\\slash"),
                Arguments.of("no special chars", "no special chars"));
    }
}
