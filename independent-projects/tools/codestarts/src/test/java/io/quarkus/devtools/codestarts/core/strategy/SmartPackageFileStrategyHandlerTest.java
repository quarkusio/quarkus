package io.quarkus.devtools.codestarts.core.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmartPackageFileStrategyHandlerTest {

    @Test
    void testRefactorJava() {
        assertThat(SmartPackageFileStrategyHandler.refactorPackage(
                "package org.acme.qute;\n\nimport org.acme.qute.Something\nimport jakarta.ws.rs.core.MediaType\n",
                "my.java.app")).isEqualTo(
                        "package my.java.app.qute;\n\nimport my.java.app.qute.Something\nimport jakarta.ws.rs.core.MediaType\n");
    }

    @Test
    void testRefactorKotlin() {
        assertThat(SmartPackageFileStrategyHandler.refactorPackage(
                "package org.acme.qute\n\nimport org.acme.qute.Something\nimport jakarta.ws.rs.core.MediaType\n",
                "my.kotlin.app")).isEqualTo(
                        "package my.kotlin.app.qute\n\nimport my.kotlin.app.qute.Something\nimport jakarta.ws.rs.core.MediaType\n");
    }

    @Test
    void testRefactorScala() {
        assertThat(SmartPackageFileStrategyHandler.refactorPackage(
                "package org.acme.qute\n\nimport org.acme.qute.Something\nimport org.acme.qute.\\{GET, Path, Produces}\nimport jakarta.ws.rs.core.MediaType\n",
                "my.scala.app")).isEqualTo(
                        "package my.scala.app.qute\n\nimport my.scala.app.qute.Something\nimport my.scala.app.qute.\\{GET, Path, Produces}\nimport jakarta.ws.rs.core.MediaType\n");
    }
}
