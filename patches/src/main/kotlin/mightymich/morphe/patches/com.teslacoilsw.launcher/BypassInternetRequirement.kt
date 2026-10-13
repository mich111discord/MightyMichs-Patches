package mightymich.morphe.patches.com.teslacoilsw.launcher.BypassInternetRequirement

import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import app.morphe.patcher.patch.bytecodePatch
import mightymich.morphe.patches.com.teslacoilsw.launcher.NovaLauncherCompatibility

@Suppress("unused")
val bypassInternetRequirementPatch = bytecodePatch(
    name = "Safe Remove Internet Permission",
    description = "Safely removes the INTERNET permission and stubs network checks to prevent search crashes.",
    default = true
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)

    execute {
        manifest.removePermission("android.permission.INTERNET")

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.name.contains("isNetworkAvailable", ignoreCase = true) ||
                    method.name.contains("isConnected", ignoreCase = true)) {
                    try {
                        method.addInstructions(
                            0,
                            """
                                const/4 v0, 0x1
                                return v0
                            """
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}
