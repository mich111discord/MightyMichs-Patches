package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.options.StringOption

@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change Package Name",
    description = "Safely changes the package name of Nova Launcher to prevent conflicts and system crashes.",
    default = false
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)

    val newPackageName by option(
        StringOption(
            key = "packageName",
            title = "New Package Name",
            description = "The target package name for Nova Launcher.",
            required = true,
            default = "com.teslacoilsw.launcher.mightymich"
        )
    )

    execute {
        val targetPackage = newPackageName ?: "com.teslacoilsw.launcher.mightymich"
        document.packageName = targetPackage
    }
}
