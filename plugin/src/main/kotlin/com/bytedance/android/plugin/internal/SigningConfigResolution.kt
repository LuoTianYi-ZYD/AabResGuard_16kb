package com.bytedance.android.plugin.internal

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.variant.ApplicationVariant
import com.bytedance.android.plugin.model.SigningConfig

/**
 * Created by YangJing on 2020/01/06 .
 * Email: yangjing.yeoh@bytedance.com
 */
internal fun getSigningConfig(android: ApplicationExtension, variant: ApplicationVariant): SigningConfig {
    val buildType = variant.buildType ?: return SigningConfig(null, null, null, null)
    val buildTypeSigningConfig = android.buildTypes.findByName(buildType)?.signingConfig
    val signingConfig = buildTypeSigningConfig ?: android.signingConfigs.findByName(buildType)
    return getSigningConfig(signingConfig)
}

private fun getSigningConfig(signingConfig: ApkSigningConfig?): SigningConfig {
    return SigningConfig(
            signingConfig?.storeFile,
            signingConfig?.storePassword,
            signingConfig?.keyAlias,
            signingConfig?.keyPassword
    )
}
