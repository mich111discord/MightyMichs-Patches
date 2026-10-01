package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object NovaLauncherCompatibility {
    val NOVA_LAUNCHER = Compatibility(
        name = "Nova Launcher",
        packageName = "com.teslacoilsw.launcher",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF2196F3.toInt(),
        description = "Unlocks Nova Launcher Prime features.",
        signatures = null,
        isLegacy = false,
        targets = listOf(
            AppTarget(version = "8.8.9")
        )
    )
}
