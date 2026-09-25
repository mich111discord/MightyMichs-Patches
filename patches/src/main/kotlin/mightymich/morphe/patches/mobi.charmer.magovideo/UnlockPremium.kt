package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the purchase flag to true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // Fingerprint: locate the SPUT_BOOLEAN instruction that writes to the static field "Z" in class "Lf2/l;".
    val proFlagFingerprint = Fingerprint(
        filters = listOf(
            fieldAccess(
                opcode = Opcode.SPUT_BOOLEAN,
                definingClass = "Lf2/l;",
                name = "Z"
            )
        )
    )

    execute {
        proFlagFingerprint.let { fingerprint ->
            // Get the first (and only) match for this field access.
            val match = fingerprint.instructionMatches.firstOrNull()
                ?: throw PatchException("Could not find the SPUT_BOOLEAN instruction for Lf2/l;->Z:Z")

            // Get the register that holds the value to be stored.
            val instruction = match.getInstruction<OneRegisterInstruction>()
            val register = instruction.registerA

            // Insert "const/4 vX, 0x1" immediately before the SPUT_BOOLEAN.
            // This forces the field to be set to true (1) regardless of the original value.
            fingerprint.method.addInstructions(
                match.index,
                "const/4 v$register, 0x1"
            )
        }
    }
}
