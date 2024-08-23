package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.builder.Version
import io.quarkus.test.ProdBuildResults
import io.quarkus.test.ProdModeTestResults
import io.quarkus.test.QuarkusProdModeTest
import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Verifies that DDL scripts are generated when script generation is configured in
 * application.properties.
 */
class DDLGenerationPMT {
    @ProdBuildResults var prodModeTestResults: ProdModeTestResults? = null

    @Test
    fun test() {
        RestAssured.`when`()["/no-paging-test"].then().body(Matchers.`is`("OK"))
        Assertions.assertThat(
                prodModeTestResults!!.buildDir.resolve("quarkus-app/create.sql").toFile()
            )
            .exists()
        Assertions.assertThat(
                prodModeTestResults!!.buildDir.resolve("quarkus-app/drop.sql").toFile()
            )
            .exists()
    }

    companion object {
        @RegisterExtension
        val config =
            QuarkusProdModeTest()
                .withApplicationRoot { jar: JavaArchive ->
                    jar.addClasses(PageItem::class.java, NoPagingTestEndpoint::class.java)
                }
                .setApplicationName("ddl-generation")
                .setApplicationVersion(Version.getVersion())
                .setRun(true)
                .setLogFileName("ddl-generation-test.log")
                .withConfigurationResource("ddlgeneration.properties")
    }
}
