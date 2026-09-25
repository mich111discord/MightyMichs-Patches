package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the purchase flag to true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the method that contains the string "onetime_purchase".
    //    This method is responsible for initializing the purchase state.
    val purchaseInitFingerprint = Fingerprint(
        filters = listOf(
            string("onetime_purchase"),
            returnType = "V" // The method e(Activity) returns void.
        )
    )

    execute {
        purchaseInitFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val implementation = method.implementation
                ?: throw PatchException("Method has no implementation.")

            // 2. Find the end of the method to append our instructions.
            val endIndex = implementation.instructions.size

            // 3. Append instructions that force the premium flag to true.
            //    We use v0 as a scratch register (it is safe to use at the end of a void method).
            method.addInstructions(
                endIndex,
                """
                    const/4 v0, 0x1
                    sput-boolean v0, Lf2/l;->Z:Z
                """
            )
        }
    }
}package mightymich.morphe.patches.magovideo

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
