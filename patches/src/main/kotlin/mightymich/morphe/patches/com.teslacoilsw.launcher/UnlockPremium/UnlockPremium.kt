package mightymich.morphe.patches.com.teslacoilsw.launcher.UnlockPremium

import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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
val unlockPremiumPatch = bytecodePatch(
    name = "Enable Prime Features",
    description = "Unlocks Nova Launcher Prime functionality.",
    default = true
) {
    compatibleWith(NovaCompatibility.INSTANCE)

    execute {
        val primeCheckClasses = listOf(
            "Lcom/teslacoilsw/launcher/prime/PrimeUtils;",
            "Lcom/teslacoilsw/launcher/PrimeHelper;"
        )

        for (className in primeCheckClasses) {
            val classDef = classes.find { it.type == className } ?: continue
            for (method in classDef.methods) {
                if (method.name.contains("isPrimeInstalled", ignoreCase = true) ||
                    method.name.contains("isPrimeUnlocked", ignoreCase = true)) {
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
