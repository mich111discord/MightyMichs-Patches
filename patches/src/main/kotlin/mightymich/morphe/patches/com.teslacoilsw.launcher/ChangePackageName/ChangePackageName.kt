package mightymich.morphe.patches.com.teslacoilsw.launcher.ChangePackageName

import app.morphe.patcher.annotation.Option
import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.options.StringOption
import org.jf.dexlib2.iface.instruction.reference.StringReferenceInstruction
import org.jf.dexlib2.immutable.reference.ImmutableStringReference

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
val changePackageNamePatch = bytecodePatch(
    name = "Change Package Name",
    description = "Safely changes the package name of Nova Launcher, updating all internal authorities, permissions, and string references to prevent crashes.",
    default = false
) {
    compatibleWith(NovaCompatibility.INSTANCE)

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
        val oldPackage = "com.teslacoilsw.launcher"
        val targetPackage = newPackageName ?: "com.teslacoilsw.launcher.mightymich"

        manifest.packageName = targetPackage

        val oldAuthority = "$oldPackage.grid"
        val newAuthority = "$targetPackage.grid"

        val oldPermission = "$oldPackage.permission"
        val newPermission = "$targetPackage.permission"

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.instructions.forEach { instruction ->
                    if (instruction is StringReferenceInstruction) {
                        val originalString = instruction.reference.string
                        if (originalString.contains(oldPackage)) {
                            val updatedString = originalString
                                .replace(oldAuthority, newAuthority)
                                .replace(oldPermission, newPermission)
                                .replace(oldPackage, targetPackage)

                            if (updatedString != originalString) {
                                instruction.replaceReference(
                                    ImmutableStringReference(updatedString)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
