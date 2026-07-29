package com.bytedance.android.plugin.tasks

import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.model.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Path

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 * Modified 2021/08/11
 */
open class AabResGuardTask : DefaultTask() {

    @get:Internal
    private lateinit var variantName: String

    @get:Internal
    lateinit var signingConfig: SigningConfig

    @get:Internal
    var aabResGuard: AabResGuardExtension = project.extensions.getByName("aabResGuard") as AabResGuardExtension

    @get:InputFile
    val bundleFile: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    private lateinit var bundlePath: Path

    @get:Internal
    private lateinit var obfuscatedBundlePath: Path

    init {
        description = "Assemble resource proguard for bundle file"
        group = "bundle"
        outputs.upToDateWhen { false }
    }

    fun setVariantScope(variantName: String, bundleProvider: Provider<RegularFile>, signingConfig: SigningConfig) {
        this.variantName = variantName
        this.bundleFile.set(bundleProvider)
        this.signingConfig = signingConfig
    }
/*
    @InputFile
    @Optional
    fun getObfuscatedBundlePath(): Path {
        return obfuscatedBundlePath
    }
*/

    @TaskAction
    fun runAabResGuard() {
        println(aabResGuard.toString())
        bundlePath = bundleFile.get().asFile.toPath()
        obfuscatedBundlePath = File(bundlePath.toFile().parentFile, aabResGuard.obfuscatedBundleFileName).toPath()
        printSignConfiguration()
        printOutputFileLocation()

        prepareUnusedFile()

        val command = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(aabResGuard.enableObfuscate)
                .setBundlePath(bundlePath)
                .setOutputPath(obfuscatedBundlePath)
                .setMergeDuplicatedResources(aabResGuard.mergeDuplicatedRes)
                .setWhiteList(aabResGuard.whiteList)
                .setFilterFile(aabResGuard.enableFilterFiles)
                .setFileFilterRules(aabResGuard.filterList)
                .setRemoveStr(aabResGuard.enableFilterStrings)
                .setUnusedStrPath(aabResGuard.unusedStringPath)
                .setLanguageWhiteList(aabResGuard.languageWhiteList)
        if (aabResGuard.mappingFile != null) {
            command.setMappingPath(aabResGuard.mappingFile)
        }

        if (signingConfig.storeFile != null && signingConfig.storeFile!!.exists()) {
            command.setStoreFile(signingConfig.storeFile!!.toPath())
                    .setKeyAlias(signingConfig.keyAlias)
                    .setKeyPassword(signingConfig.keyPassword)
                    .setStorePassword(signingConfig.storePassword)
        }
        command.build().execute()
    }

    private fun prepareUnusedFile() {
        val resourcePath = project.layout.buildDirectory.file("outputs/mapping/$variantName/unused.txt").get().asFile
        val usedFile = resourcePath
        if (usedFile.exists()) {
            println("find unused.txt : ${usedFile.absolutePath}")
            if (aabResGuard.enableFilterStrings) {
                if (aabResGuard.unusedStringPath == null || aabResGuard.unusedStringPath!!.isBlank()) {
                    aabResGuard.unusedStringPath = usedFile.absolutePath
                    logger.warn("replace unused.txt!")
                }
            }
        } else {
            logger.warn("not exists unused.txt : ${usedFile.absolutePath}\n" +
                    "use default path : ${aabResGuard.unusedStringPath}")
        }
    }

    private fun printSignConfiguration() {
        println("-------------- Sign configuration --------------")
        println("\tStoreFile:\t\t${signingConfig.storeFile}")
        println("\tKeyPassword:\t${encrypt(signingConfig.keyPassword)}")
        println("\tAlias:\t\t\t${encrypt(signingConfig.keyAlias)}")
        println("\tStorePassword:\t${encrypt(signingConfig.storePassword)}")
    }

    private fun printOutputFileLocation() {
        println("-------------- Output configuration --------------")
        println("\tFolder:\t\t${obfuscatedBundlePath.parent}")
        println("\tFile:\t\t${obfuscatedBundlePath.fileName}")
        println("--------------------------------------------------")
    }

    private fun encrypt(value: String?): String {
        if (value == null) return "/"
        if (value.length > 2) {
            return "${value.substring(0, value.length / 2)}****"
        }
        return "****"
    }
}