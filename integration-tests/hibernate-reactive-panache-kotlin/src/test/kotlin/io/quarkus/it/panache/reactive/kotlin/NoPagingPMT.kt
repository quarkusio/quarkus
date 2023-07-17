package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.builder.Version
import io.quarkus.test.LogFile
import io.quarkus.test.QuarkusProdModeTest
import io.restassured.RestAssured
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.hamcrest.Matchers
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Test if PanacheQuery is using unnecessary SQL for limiting the number of output rows, the log
 * output is tested for the presence of `offset` or `limit` in the SQL.
 */
class NoPagingPMT {
    @LogFile private val logfile: Path? = null

    @Test
    fun test() {
        Assertions.assertThat(logfile).isRegularFile.hasFileName("no-paging-test.log")
        RestAssured.`when`()["/no-paging-test"].then().body(Matchers.`is`("OK"))

        // the logs might not be flushed to disk immediately, so wait a few seconds before giving up
        // completely
        Awaitility.await().atMost(3, TimeUnit.SECONDS).untilAsserted { checkLog() }
    }

    private fun checkLog() {
        val lines: List<String> = Files.readAllLines(logfile)

        /*
         * Test the SQL was logged, this could fail if Hibernate decides to change how it logs the generated SQL, here in order
         * to not silently skip the following test
         */
        val sqlFound =
            lines
                .stream()
                .filter { line -> line.matches(Regex(".*select .* from PageItem .*")) }
                .findAny()
                .isPresent
        Assertions.assertThat(sqlFound).`as`("Hibernate query wasn't logged").isTrue

        // Search for the presence of a SQL with a limit or offset
        val wrongSqlFound =
            lines
                .stream()
                .filter { line: String ->
                    line.matches(Regex(".*select .* limit .*")) ||
                        line.matches(Regex(".*select .* offset .*"))
                }
                .findAny()
                .isPresent
        Assertions.assertThat(wrongSqlFound)
            .`as`(
                "PanacheQuery is generating SQL with limits and/or offsets when no paging has been requested"
            )
            .isFalse
    }

    companion object {
        @RegisterExtension
        val config =
            QuarkusProdModeTest()
                .withApplicationRoot { jar: JavaArchive ->
                    jar.addClasses(PageItem::class.java, NoPagingTestEndpoint::class.java)
                }
                .setApplicationName("no-paging-test")
                .setApplicationVersion(Version.getVersion())
                .setRun(true)
                .setLogFileName("no-paging-test.log")
                .withConfigurationResource("nopaging.properties")
    }
}
