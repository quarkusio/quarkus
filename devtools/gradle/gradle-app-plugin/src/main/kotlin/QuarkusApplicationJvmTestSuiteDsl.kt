@file:Suppress("unused") // public DSL

import io.quarkus.gradle.application.QuarkusApplicationPlugin
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite
import io.quarkus.gradle.application.dsl.QuarkusApplicationStartupArchiveTraining
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.jvm.JvmTestSuite

fun JvmTestSuite.forQuarkusTests() {
    quarkusApplicationJvmTestSuite().forQuarkusTests()
}

fun JvmTestSuite.forQuarkusIntegrationTests(build: Any) {
    quarkusApplicationJvmTestSuite().forQuarkusIntegrationTests(build)
}

/**
 * Configures this package-backed integration-test suite to train the selected named AOT or SCC
 * build.
 *
 * The action must select exactly one execution target. It may be called before or after
 * [forQuarkusIntegrationTests], but the suite must ultimately select exactly one integration-test
 * build and cannot also use ordinary Quarkus JVM-test or generated native-test mode.
 *
 * @param action the startup-archive training configuration
 */
fun JvmTestSuite.startupArchiveTraining(
    action: Action<in QuarkusApplicationStartupArchiveTraining>
) {
    quarkusApplicationJvmTestSuite().startupArchiveTraining(action)
}

fun JvmTestSuite.includeTestsFrom(suite: NamedDomainObjectProvider<out JvmTestSuite>) {
    quarkusApplicationJvmTestSuite().includeTestsFrom(suite)
}

private fun JvmTestSuite.quarkusApplicationJvmTestSuite(): QuarkusApplicationJvmTestSuite =
    (this as ExtensionAware).extensions.getByName(QuarkusApplicationPlugin.EXTENSION_NAME)
        as QuarkusApplicationJvmTestSuite
