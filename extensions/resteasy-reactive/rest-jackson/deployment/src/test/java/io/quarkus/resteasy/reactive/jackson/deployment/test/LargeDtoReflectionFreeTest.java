package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * A DTO with so many properties that the generated serializer exceeds the ASM frame computation limit must not
 * break the build: the reflection-based serializer is used for it.
 */
public class LargeDtoReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Part.class, Large.class, LargeResource.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            })
            .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING)
                    && record.getMessage().contains("because it is too large"))
            .assertLogRecords(records -> assertThat(records).hasSize(1));

    @Test
    public void testLargeDto() {
        RestAssured.get("/large")
                .then()
                .statusCode(200)
                .body("p0.name", is("n"))
                .body("p149.amount", is(1))
                .body("p149.names.size()", is(1));
    }

    @Path("/large")
    public static class LargeResource {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Large get() {
            return new Large();
        }
    }

    public static class Part {
        public boolean flag = true;
        public Boolean boxed = true;
        public BigDecimal amount = BigDecimal.ONE;
        public String name = "n";
        public List<String> names = List.of("a");
    }

    public static class Large {
        public Part p0 = new Part();
        public Part p1 = new Part();
        public Part p2 = new Part();
        public Part p3 = new Part();
        public Part p4 = new Part();
        public Part p5 = new Part();
        public Part p6 = new Part();
        public Part p7 = new Part();
        public Part p8 = new Part();
        public Part p9 = new Part();
        public Part p10 = new Part();
        public Part p11 = new Part();
        public Part p12 = new Part();
        public Part p13 = new Part();
        public Part p14 = new Part();
        public Part p15 = new Part();
        public Part p16 = new Part();
        public Part p17 = new Part();
        public Part p18 = new Part();
        public Part p19 = new Part();
        public Part p20 = new Part();
        public Part p21 = new Part();
        public Part p22 = new Part();
        public Part p23 = new Part();
        public Part p24 = new Part();
        public Part p25 = new Part();
        public Part p26 = new Part();
        public Part p27 = new Part();
        public Part p28 = new Part();
        public Part p29 = new Part();
        public Part p30 = new Part();
        public Part p31 = new Part();
        public Part p32 = new Part();
        public Part p33 = new Part();
        public Part p34 = new Part();
        public Part p35 = new Part();
        public Part p36 = new Part();
        public Part p37 = new Part();
        public Part p38 = new Part();
        public Part p39 = new Part();
        public Part p40 = new Part();
        public Part p41 = new Part();
        public Part p42 = new Part();
        public Part p43 = new Part();
        public Part p44 = new Part();
        public Part p45 = new Part();
        public Part p46 = new Part();
        public Part p47 = new Part();
        public Part p48 = new Part();
        public Part p49 = new Part();
        public Part p50 = new Part();
        public Part p51 = new Part();
        public Part p52 = new Part();
        public Part p53 = new Part();
        public Part p54 = new Part();
        public Part p55 = new Part();
        public Part p56 = new Part();
        public Part p57 = new Part();
        public Part p58 = new Part();
        public Part p59 = new Part();
        public Part p60 = new Part();
        public Part p61 = new Part();
        public Part p62 = new Part();
        public Part p63 = new Part();
        public Part p64 = new Part();
        public Part p65 = new Part();
        public Part p66 = new Part();
        public Part p67 = new Part();
        public Part p68 = new Part();
        public Part p69 = new Part();
        public Part p70 = new Part();
        public Part p71 = new Part();
        public Part p72 = new Part();
        public Part p73 = new Part();
        public Part p74 = new Part();
        public Part p75 = new Part();
        public Part p76 = new Part();
        public Part p77 = new Part();
        public Part p78 = new Part();
        public Part p79 = new Part();
        public Part p80 = new Part();
        public Part p81 = new Part();
        public Part p82 = new Part();
        public Part p83 = new Part();
        public Part p84 = new Part();
        public Part p85 = new Part();
        public Part p86 = new Part();
        public Part p87 = new Part();
        public Part p88 = new Part();
        public Part p89 = new Part();
        public Part p90 = new Part();
        public Part p91 = new Part();
        public Part p92 = new Part();
        public Part p93 = new Part();
        public Part p94 = new Part();
        public Part p95 = new Part();
        public Part p96 = new Part();
        public Part p97 = new Part();
        public Part p98 = new Part();
        public Part p99 = new Part();
        public Part p100 = new Part();
        public Part p101 = new Part();
        public Part p102 = new Part();
        public Part p103 = new Part();
        public Part p104 = new Part();
        public Part p105 = new Part();
        public Part p106 = new Part();
        public Part p107 = new Part();
        public Part p108 = new Part();
        public Part p109 = new Part();
        public Part p110 = new Part();
        public Part p111 = new Part();
        public Part p112 = new Part();
        public Part p113 = new Part();
        public Part p114 = new Part();
        public Part p115 = new Part();
        public Part p116 = new Part();
        public Part p117 = new Part();
        public Part p118 = new Part();
        public Part p119 = new Part();
        public Part p120 = new Part();
        public Part p121 = new Part();
        public Part p122 = new Part();
        public Part p123 = new Part();
        public Part p124 = new Part();
        public Part p125 = new Part();
        public Part p126 = new Part();
        public Part p127 = new Part();
        public Part p128 = new Part();
        public Part p129 = new Part();
        public Part p130 = new Part();
        public Part p131 = new Part();
        public Part p132 = new Part();
        public Part p133 = new Part();
        public Part p134 = new Part();
        public Part p135 = new Part();
        public Part p136 = new Part();
        public Part p137 = new Part();
        public Part p138 = new Part();
        public Part p139 = new Part();
        public Part p140 = new Part();
        public Part p141 = new Part();
        public Part p142 = new Part();
        public Part p143 = new Part();
        public Part p144 = new Part();
        public Part p145 = new Part();
        public Part p146 = new Part();
        public Part p147 = new Part();
        public Part p148 = new Part();
        public Part p149 = new Part();
    }
}
