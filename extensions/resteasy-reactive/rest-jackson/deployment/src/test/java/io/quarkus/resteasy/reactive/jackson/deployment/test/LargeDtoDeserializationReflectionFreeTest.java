package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
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
 * A DTO with so many properties that the generated code exceeds the ASM frame computation limit must not
 * break the build: the reflection-based deserialization is used for it.
 */
public class LargeDtoDeserializationReflectionFreeTest {

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
    public void testLargeDtoDeserialization() {
        StringBuilder body = new StringBuilder("{");
        for (int i = 0; i < 300; i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append("\"p").append(i).append("\":{\"flag\":true,\"boxed\":false,\"amount\":").append(i)
                    .append(",\"name\":\"n").append(i).append("\",\"names\":[\"a\",\"b\"]}");
        }
        body.append('}');

        RestAssured
                .with()
                .body(body.toString())
                .contentType("application/json")
                .post("/large")
                .then()
                .statusCode(200)
                .body(is("n0,n299,299,2"));
    }

    @Path("/large")
    public static class LargeResource {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(Large large) {
            return large.p0.name + "," + large.p299.name + "," + large.p299.amount + "," + large.p299.names.size();
        }
    }

    public static class Part {
        public boolean flag;
        public Boolean boxed;
        public BigDecimal amount;
        public String name;
        public List<String> names;
    }

    public static class Large {
        public Part p0;
        public Part p1;
        public Part p2;
        public Part p3;
        public Part p4;
        public Part p5;
        public Part p6;
        public Part p7;
        public Part p8;
        public Part p9;
        public Part p10;
        public Part p11;
        public Part p12;
        public Part p13;
        public Part p14;
        public Part p15;
        public Part p16;
        public Part p17;
        public Part p18;
        public Part p19;
        public Part p20;
        public Part p21;
        public Part p22;
        public Part p23;
        public Part p24;
        public Part p25;
        public Part p26;
        public Part p27;
        public Part p28;
        public Part p29;
        public Part p30;
        public Part p31;
        public Part p32;
        public Part p33;
        public Part p34;
        public Part p35;
        public Part p36;
        public Part p37;
        public Part p38;
        public Part p39;
        public Part p40;
        public Part p41;
        public Part p42;
        public Part p43;
        public Part p44;
        public Part p45;
        public Part p46;
        public Part p47;
        public Part p48;
        public Part p49;
        public Part p50;
        public Part p51;
        public Part p52;
        public Part p53;
        public Part p54;
        public Part p55;
        public Part p56;
        public Part p57;
        public Part p58;
        public Part p59;
        public Part p60;
        public Part p61;
        public Part p62;
        public Part p63;
        public Part p64;
        public Part p65;
        public Part p66;
        public Part p67;
        public Part p68;
        public Part p69;
        public Part p70;
        public Part p71;
        public Part p72;
        public Part p73;
        public Part p74;
        public Part p75;
        public Part p76;
        public Part p77;
        public Part p78;
        public Part p79;
        public Part p80;
        public Part p81;
        public Part p82;
        public Part p83;
        public Part p84;
        public Part p85;
        public Part p86;
        public Part p87;
        public Part p88;
        public Part p89;
        public Part p90;
        public Part p91;
        public Part p92;
        public Part p93;
        public Part p94;
        public Part p95;
        public Part p96;
        public Part p97;
        public Part p98;
        public Part p99;
        public Part p100;
        public Part p101;
        public Part p102;
        public Part p103;
        public Part p104;
        public Part p105;
        public Part p106;
        public Part p107;
        public Part p108;
        public Part p109;
        public Part p110;
        public Part p111;
        public Part p112;
        public Part p113;
        public Part p114;
        public Part p115;
        public Part p116;
        public Part p117;
        public Part p118;
        public Part p119;
        public Part p120;
        public Part p121;
        public Part p122;
        public Part p123;
        public Part p124;
        public Part p125;
        public Part p126;
        public Part p127;
        public Part p128;
        public Part p129;
        public Part p130;
        public Part p131;
        public Part p132;
        public Part p133;
        public Part p134;
        public Part p135;
        public Part p136;
        public Part p137;
        public Part p138;
        public Part p139;
        public Part p140;
        public Part p141;
        public Part p142;
        public Part p143;
        public Part p144;
        public Part p145;
        public Part p146;
        public Part p147;
        public Part p148;
        public Part p149;
        public Part p150;
        public Part p151;
        public Part p152;
        public Part p153;
        public Part p154;
        public Part p155;
        public Part p156;
        public Part p157;
        public Part p158;
        public Part p159;
        public Part p160;
        public Part p161;
        public Part p162;
        public Part p163;
        public Part p164;
        public Part p165;
        public Part p166;
        public Part p167;
        public Part p168;
        public Part p169;
        public Part p170;
        public Part p171;
        public Part p172;
        public Part p173;
        public Part p174;
        public Part p175;
        public Part p176;
        public Part p177;
        public Part p178;
        public Part p179;
        public Part p180;
        public Part p181;
        public Part p182;
        public Part p183;
        public Part p184;
        public Part p185;
        public Part p186;
        public Part p187;
        public Part p188;
        public Part p189;
        public Part p190;
        public Part p191;
        public Part p192;
        public Part p193;
        public Part p194;
        public Part p195;
        public Part p196;
        public Part p197;
        public Part p198;
        public Part p199;
        public Part p200;
        public Part p201;
        public Part p202;
        public Part p203;
        public Part p204;
        public Part p205;
        public Part p206;
        public Part p207;
        public Part p208;
        public Part p209;
        public Part p210;
        public Part p211;
        public Part p212;
        public Part p213;
        public Part p214;
        public Part p215;
        public Part p216;
        public Part p217;
        public Part p218;
        public Part p219;
        public Part p220;
        public Part p221;
        public Part p222;
        public Part p223;
        public Part p224;
        public Part p225;
        public Part p226;
        public Part p227;
        public Part p228;
        public Part p229;
        public Part p230;
        public Part p231;
        public Part p232;
        public Part p233;
        public Part p234;
        public Part p235;
        public Part p236;
        public Part p237;
        public Part p238;
        public Part p239;
        public Part p240;
        public Part p241;
        public Part p242;
        public Part p243;
        public Part p244;
        public Part p245;
        public Part p246;
        public Part p247;
        public Part p248;
        public Part p249;
        public Part p250;
        public Part p251;
        public Part p252;
        public Part p253;
        public Part p254;
        public Part p255;
        public Part p256;
        public Part p257;
        public Part p258;
        public Part p259;
        public Part p260;
        public Part p261;
        public Part p262;
        public Part p263;
        public Part p264;
        public Part p265;
        public Part p266;
        public Part p267;
        public Part p268;
        public Part p269;
        public Part p270;
        public Part p271;
        public Part p272;
        public Part p273;
        public Part p274;
        public Part p275;
        public Part p276;
        public Part p277;
        public Part p278;
        public Part p279;
        public Part p280;
        public Part p281;
        public Part p282;
        public Part p283;
        public Part p284;
        public Part p285;
        public Part p286;
        public Part p287;
        public Part p288;
        public Part p289;
        public Part p290;
        public Part p291;
        public Part p292;
        public Part p293;
        public Part p294;
        public Part p295;
        public Part p296;
        public Part p297;
        public Part p298;
        public Part p299;
    }
}
