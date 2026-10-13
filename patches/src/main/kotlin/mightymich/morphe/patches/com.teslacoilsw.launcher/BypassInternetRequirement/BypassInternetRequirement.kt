package mightymich.morphe.patches.com.teslacoilsw.launcher.BypassInternetRequirement

import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private object NovaCompatibility {
    val INSTANCE = Compatibility(
        name = "Nova Launcher",
        packageName = "com.teslacoilsw.launcher",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2196F3,
        targets = listOf(
            AppTarget(version = "7.0.58"),
            AppTarget(version = "8.0.8"),
            AppTarget(version = "8.8.8")
        )
    )
}

@Suppress("unused")
val bypassInternetRequirementPatch = bytecodePatch(
    name = "Safe Remove Internet Permission",
    description = "Safely removes the INTERNET permission from the manifest and stubs out network checks to prevent search and background crashes.",
    default = true
) {
    compatibleWith(NovaCompatibility.INSTANCE)

    execute {
        manifest.removePermission("android.permission.INTERNET")

        val networkCheckClasses = listOf(
            "Lcom/teslacoilsw/launcher/network/NetworkUtils;",
            "Lcom/teslacoilsw/launcher/search/SearchNetworkHelper;"
        )

        for (className in networkCheckClasses) {
            val classDef = classes.find { it.type == className } ?: continue
            for (method in classDef.methods) {
                if (method.name.contains("isNetworkAvailable", ignoreCase = true) ||
                    method.name.contains("isConnected", ignoreCase = true)) {
                    method.addInstructions(
                        0,
                        """
                            const/4 v0, 0x1
                            return v0
                        """
                    )
                }
            }
        }
    }
}
