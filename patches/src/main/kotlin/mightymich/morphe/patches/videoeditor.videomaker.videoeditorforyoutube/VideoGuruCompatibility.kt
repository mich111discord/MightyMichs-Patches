package mightymich.morphe.patches.videoguru

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object VideoGuruCompatibility {
    val VIDEO_GURU = Compatibility(
        name = "Video Guru",
        packageName = "videoeditor.videomaker.videoeditorforyoutube",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF9800, // Orange color, typical for video editing apps.
        targets = listOf(
            AppTarget(version = "1.371.93")
            AppTarget(version = "1.621.196") // Tested version.
        )
    )
}
