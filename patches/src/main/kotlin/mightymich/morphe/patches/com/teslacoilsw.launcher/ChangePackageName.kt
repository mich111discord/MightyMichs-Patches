package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.framework.patcher.annotation.Patch
import app.morphe.framework.patcher.annotation.Option
import app.morphe.framework.patcher.BytecodePatch
import app.morphe.framework.patcher.BytecodeContext
import app.morphe.framework.patcher.Option.StringOption
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import org.jf.dexlib2.iface.instruction.reference.StringReferenceInstruction
import org.jf.dexlib2.immutable.reference.ImmutableStringReference

@Patch(
    name = "Change Package Name",
    description = "Safely changes the package name of Nova Launcher, updating all internal authorities, permissions, and string references to prevent crashes."
)
object ChangePackageName : BytecodePatch(
    setOf(PackageNamePatchContext)
) {
    @Option(
        key = "packageName",
        name = "New Package Name",
        description = "The target package name for Nova Launcher (e.g. com.teslacoilsw.launcher.mightymich).",
        required = true,
        default = "com.teslacoilsw.launcher.mightymich"
    )
    lateinit var newPackageName: StringOption
}

object PackageNamePatchContext : BytecodeContext() {
    init {
        execute {
            val oldPackage = "com.teslacoilsw.launcher"
            val targetPackage = ChangePackageName.newPackageName.value ?: "com.teslacoilsw.launcher.morphe"

            manifest.packageName = targetPackage

            val oldAuthority = "$oldPackage.grid"
            val newAuthority = "$targetPackage.grid"

            val oldPermission = "$oldPackage.permission"
            val newPermission = "$targetPackage.permission"

            for (classDef in mutableClasses) {
                for (method in classDef.methods) {
                    val implementation = method.implementation ?: continue
                    for (instruction in implementation.instructions) {
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
}
