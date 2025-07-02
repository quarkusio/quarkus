package io.quarkus.vertx.http.runtime.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.security.Basic;

public class BasicTest {

    @Test
    void testBasicBuilder() {
        Basic enabledBasic = Basic.enable();
        Assertions.assertTrue(enabledBasic.enabled().orElseThrow());
        Basic realmBasic = Basic.realm("that-realm");
        Assertions.assertTrue(realmBasic.enabled().orElseThrow());
        Assertions.assertEquals("that-realm", realmBasic.realm());
    }

}
