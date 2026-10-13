package mightymich.morphe.patches.com.teslacoilsw.launcher.ChangePackageName

import app.morphe.patcher.annotation.Patch
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.options.StringOption
import mightymich.morphe.patches.com.teslacoilsw.launcher.NovaLauncherCompatibility
import org.jf.dexlib2.iface.instruction.reference.StringReferenceInstruction
import org.jf.dexlib2.immutable.reference.ImmutableStringReference

@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change Package Name",
    description = "Safely changes the package name of Nova Launcher to prevent conflicts and system crashes.",
    default = true
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
        val oldPackage = "com.teslacoilsw.launcher"
        val targetPackage = newPackageName ?: "com.teslacoilsw.launcher.mightymich"

        manifest.packageName = targetPackage

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation?.instructions?.forEach { instruction ->
                    if (instruction is StringReferenceInstruction) {
                        val originalString = instruction.reference.string
                        if (originalString.contains(oldPackage)) {
                            val updatedString = originalString.replace(oldPackage, targetPackage)
                            if (updatedString != originalString) {
                                instruction.replaceReference(ImmutableStringReference(updatedString))
                            }
                        }
                    }
                }
            }
        }
    }
}
