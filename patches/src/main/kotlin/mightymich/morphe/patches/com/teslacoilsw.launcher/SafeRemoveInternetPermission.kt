package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.framework.patcher.annotation.Patch
import app.morphe.framework.patcher.BytecodePatch
import app.morphe.framework.patcher.BytecodeContext
import app.morphe.patcher.extensions.ResourceExtensions.manifest
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x

@Patch(
    name = "Safe Remove Internet Permission",
    description = "Safely removes the INTERNET permission from the manifest and stubs out network checks to prevent search and background crashes."
)
object SafeRemoveInternetPermission : BytecodePatch(
    setOf(NetworkStubPatchContext)
)

object NetworkStubPatchContext : BytecodeContext() {
    init {
        execute {
            manifest.removePermission("android.permission.INTERNET")

            val networkCheckClasses = listOf(
                "Lcom/teslacoilsw/launcher/network/NetworkUtils;",
                "Lcom/teslacoilsw/launcher/search/SearchNetworkHelper;"
            )

            for (className in networkCheckClasses) {
                val classDef = mutableClasses.find { it.type == className } ?: continue
                for (method in classDef.methods) {
                    if (method.name.contains("isNetworkAvailable", ignoreCase = true) ||
                        method.name.contains("isConnected", ignoreCase = true)) {
                        val implementation = method.implementation ?: continue
                        implementation.instructions.clear()
                        implementation.instructions.add(
                            BuilderInstruction11n(Opcode.CONST_4, 0, 1)
                        )
                        implementation.instructions.add(
                            BuilderInstruction11x(Opcode.RETURN, 0)
                        )
                    }
                }
            }
        }
    }
}
