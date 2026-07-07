@file:Suppress("unused") // public DSL

import io.quarkus.gradle.application.QuarkusApplicationPlugin
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.jvm.JvmTestSuite

fun JvmTestSuite.forQuarkusTests() {
    quarkusApplicationJvmTestSuite().forQuarkusTests()
}

fun JvmTestSuite.forQuarkusIntegrationTests(build: Any) {
    quarkusApplicationJvmTestSuite().forQuarkusIntegrationTests(build)
}

fun JvmTestSuite.includeTestsFrom(suite: NamedDomainObjectProvider<out JvmTestSuite>) {
    quarkusApplicationJvmTestSuite().includeTestsFrom(suite)
}

private fun JvmTestSuite.quarkusApplicationJvmTestSuite(): QuarkusApplicationJvmTestSuite =
    (this as ExtensionAware).extensions.getByName(QuarkusApplicationPlugin.EXTENSION_NAME)
        as QuarkusApplicationJvmTestSuite
