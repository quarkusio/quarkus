package io.quarkus.allopen.cli

import com.google.auto.service.AutoService
import io.quarkus.allopen.cli.AllOpenCommandLineProcessor.AllOpenConfigurationKeys.ANNOTATION
import io.quarkus.allopen.cli.AllOpenCommandLineProcessor.AllOpenConfigurationKeys.PRESET
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@AutoService(CommandLineProcessor::class)
class AllOpenCommandLineProcessor : CommandLineProcessor {

    companion object {
        val SUPPORTED_PRESETS = mapOf(
                "quarkus" to listOf(
                        "javax.enterprise.context.ApplicationScoped",
                        "javax.enterprise.context.RequestScoped"))

        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                required = false, allowMultipleOccurrences = true)

        val PRESET_OPTION = CliOption("preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
                required = false, allowMultipleOccurrences = true)

        val PLUGIN_ID = "io.quarkus.quarkus-allopen"
    }

    override val pluginId: String
        get() = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption>
        get() = listOf(ANNOTATION_OPTION, PRESET_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            ANNOTATION_OPTION -> configuration.appendList(ANNOTATION, value)
            PRESET_OPTION -> configuration.appendList(PRESET, value)
            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
        }
    }

    object AllOpenConfigurationKeys {
        val ANNOTATION: CompilerConfigurationKey<List<String>> =
                CompilerConfigurationKey.create("annotation qualified name")

        val PRESET: CompilerConfigurationKey<List<String>> = CompilerConfigurationKey.create("annotation preset")
    }

}