package io.quarkus.allopen.cli

import com.google.auto.service.AutoService
import io.quarkus.allopen.cli.AllOpenCommandLineProcessor.AllOpenConfigurationKeys.ANNOTATION
import io.quarkus.allopen.cli.AllOpenCommandLineProcessor.AllOpenConfigurationKeys.PRESET
import io.quarkus.allopen.cli.AllOpenCommandLineProcessor.Companion.SUPPORTED_PRESETS
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.DeclarationAttributeAltererExtension

@AutoService(ComponentRegistrar::class)
class AllOpenComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotations = configuration.get(ANNOTATION)?.toMutableList() ?: mutableListOf()
        configuration.get(PRESET)?.forEach { preset ->
            SUPPORTED_PRESETS[preset]?.let { annotations += it }
        }
        if (annotations.isEmpty()) return

        DeclarationAttributeAltererExtension.registerExtension(project, AllOpenDeclarationAttributeAltererExtension(annotations))
    }
}