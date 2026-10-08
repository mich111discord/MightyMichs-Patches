package mightymich.morphe.patches.mobi.charmer.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks MagoVideo premium by forcing the premium check method h0 to return true.",
    default = true
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate method h0 in class Lf2/l;.
    //    It reads the static boolean field Z and returns it. 
    val premiumCheckFingerprint = Fingerprint(
        definingClass = "Lf2/l;",
        name = "h0",
        returnType = "Z"
    )

    execute {
        premiumCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method
                ?: throw PatchException("Could not find method h0 in Lf2/l;.")

            val instructions = method.implementation?.instructions?.toList()
                ?: throw PatchException("Method h0 has no implementation.")

            // 2. Find the first sget-boolean instruction and replace it
            //    with const/4 vX, 0x1 (true).
            var replaced = false
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction.opcode.name == "SGET_BOOLEAN" &&
                    instruction is OneRegisterInstruction
                ) {
                    method.replaceInstruction(
                        i,
                        "const/4 v${instruction.registerA}, 0x1"
                    )
                    replaced = true
                    break
                }
            }

            if (!replaced) {
                throw PatchException("Could not find sget-boolean in method h0.")
            }
        }
    }
}
