package io.quarkus.allopen

import com.google.auto.service.AutoService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.javax.inject.Inject

class AllOpenPlugin  : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create(artifactId, AllOpenExtension::class.java)
    }
}

const val groupId = "io.quarkus"
const val artifactId = "quarkus-allopen"
const val version = "999-SNAPSHOT"

@AutoService(KotlinGradleSubplugin::class)
class AllOpenKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        private val ANNOTATION_ARG_NAME = "annotation"
        private val PRESET_ARG_NAME = "preset"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) =
            project.plugins.hasPlugin(AllOpenPlugin::class.java)

    override fun apply(
            project: Project,
            kotlinCompile: AbstractCompile,
            javaCompile: AbstractCompile?,
            variantData: Any?,
            androidProjectHandler: Any?,
            kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {

        val allOpenExtension = project.extensions.findByType(AllOpenExtension::class.java) ?: AllOpenExtension()

        val options = mutableListOf<SubpluginOption>()

        for (anno in allOpenExtension.myAnnotations) {
            options += SubpluginOption(ANNOTATION_ARG_NAME, anno)
        }

        for (preset in allOpenExtension.myPresets) {
            options += SubpluginOption(PRESET_ARG_NAME, preset)
        }

        return options
    }

    override fun getCompilerPluginId() = "io.quarkus.quarkus-allopen"
    override fun getPluginArtifact() = SubpluginArtifact(groupId, artifactId, version)
}