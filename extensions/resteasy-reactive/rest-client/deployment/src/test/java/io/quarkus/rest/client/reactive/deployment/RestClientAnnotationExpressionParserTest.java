package io.quarkus.rest.client.reactive.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser;
import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser.Accessible;
import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser.ConfigName;
import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser.Node;
import io.quarkus.rest.client.reactive.deployment.MicroProfileRestClientEnricher.RestClientAnnotationExpressionParser.Verbatim;

public class RestClientAnnotationExpressionParserTest {

    @ParameterizedTest
    @MethodSource
    void test(String input, List<Node> expectedResult) {
        List<Node> result = new RestClientAnnotationExpressionParser(input, null).parse();
        assertThat(result).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> test() {
        return Stream.of(
                Arguments.of("", Collections.emptyList()),
                Arguments.of("only verbatim", List.of(new Verbatim("only verbatim"))),
                Arguments.of("${only.config}", List.of(new ConfigName("only.config"))),
                Arguments.of("{only.methodCall}", List.of(new ConfigName("only.methodCall"))),
                Arguments.of(
                        "first use a ${config.name} then a {methodCall} then a {fieldAccess} then another ${config} and we're done",
                        List.of(
                                new Verbatim("first use a "),
                                new ConfigName("config.name"),
                                new Verbatim(" then a "),
                                new Accessible("methodCall"),
                                new Verbatim(" then a "),
                                new Accessible("fieldAccess"),
                                new Verbatim(" then another "),
                                new ConfigName("config"),
                                new Verbatim(" and we're done"))));
    }
}
