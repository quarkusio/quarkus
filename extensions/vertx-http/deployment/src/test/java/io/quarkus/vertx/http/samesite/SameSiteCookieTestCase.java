package io.quarkus.vertx.http.samesite;

import java.util.HashMap;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.matcher.RestAssuredMatchers;

public class SameSiteCookieTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SetCookieHandler.class)
                            .addAsResource(new StringAsset(
                                    "quarkus.http.same-site-cookie.cookie1.value=Lax\n" +
                                            "quarkus.http.same-site-cookie.cookie2.value=Lax\n" +
                                            "quarkus.http.same-site-cookie.cookie2.case-sensitive=true\n" +
                                            "quarkus.http.same-site-cookie.cookie3.value=None\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testSameSiteCookie() {
        RestAssured.get("/cookie")
                .then().cookies(new HashMap<>())
                .cookie("cookie1", RestAssuredMatchers.detailedCookie().sameSite("Lax"))
                .cookie("COOKIE2", RestAssuredMatchers.detailedCookie().sameSite(Matchers.nullValue()))
                .cookie("cookie3", RestAssuredMatchers.detailedCookie().sameSite("None"))
                .cookie("cookie3", RestAssuredMatchers.detailedCookie().secured(true));

        RestAssured.with().header("user-agent", "Chromium/53 foo").get("/cookie")
                .then().cookies(new HashMap<>())
                .cookie("cookie1", RestAssuredMatchers.detailedCookie().sameSite("Lax"))
                .cookie("COOKIE2", RestAssuredMatchers.detailedCookie().sameSite(Matchers.nullValue()))
                .cookie("cookie3", RestAssuredMatchers.detailedCookie().sameSite(Matchers.nullValue()));
    }
}
