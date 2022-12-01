package io.quarkus.smallrye.graphql.deployment;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.quarkus.test.QuarkusUnitTest;

public class GraphQLTracingTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, TestPojo.class, TestRandom.class, TestGenericsPojo.class,
                            BusinessException.class, TestUnion.class, TestUnionMember.class)
                    .addAsResource(new StringAsset("quarkus.jaeger.disable-tracer-registration=true"), "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    static MockTracer mockTracer = new MockTracer();

    static {
        GlobalTracer.registerIfAbsent(mockTracer);
    }

    @BeforeEach
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testTracing() {
        pingTest();
        List<MockSpan> spans = mockTracer.finishedSpans()
                .stream()
                .filter(span -> span.operationName().equals("GraphQL:Query.ping"))
                .collect(Collectors.toList());
        Assertions.assertEquals(1, spans.size());
    }

}
