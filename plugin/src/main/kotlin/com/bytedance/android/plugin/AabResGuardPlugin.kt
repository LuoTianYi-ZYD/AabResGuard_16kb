package com.bytedance.android.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ApplicationVariantBuilder
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.internal.getSigningConfig
import com.bytedance.android.plugin.tasks.AabResGuardTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
class AabResGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        project.extensions.create("aabResGuard", AabResGuardExtension::class.java)

        val android = project.extensions.getByType(ApplicationExtension::class.java)
        @Suppress("UNCHECKED_CAST")
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
                as AndroidComponentsExtension<ApplicationExtension, ApplicationVariantBuilder, ApplicationVariant>

        androidComponents.onVariants { variant ->
            createAabResGuardTask(project, android, variant)
        }
    }

    private fun createAabResGuardTask(project: Project, android: ApplicationExtension, variant: ApplicationVariant) {
        val variantName = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val aabResGuardTaskName = "aabresguard$variantName"
        val aabResGuardTask = project.tasks.register(aabResGuardTaskName, AabResGuardTask::class.java) { task ->
            task.setVariantScope(
                    variant.name,
                    variant.artifacts.get(SingleArtifact.BUNDLE),
                    getSigningConfig(android, variant)
            )
        }

        project.tasks.matching { it.name == "bundle$variantName" }.configureEach {
            it.finalizedBy(aabResGuardTask)
        }
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw  GradleException("Android Application plugin required")
        }
    }
}
