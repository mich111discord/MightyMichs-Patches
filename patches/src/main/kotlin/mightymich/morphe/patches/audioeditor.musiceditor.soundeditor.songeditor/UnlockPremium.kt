package mightymich.morphe.patches.audioeditor.musiceditor.soundeditor.songeditor

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Forces the purchase verification method to always return true, unlocking premium features in Audio Editor."
) {
    compatibleWith(AudioEditorCompatibility.AUDIO_EDITOR)

    // 1. Define the fingerprint using an explicit instruction filter.
    val purchaseCheckFingerprint = Fingerprint(
        filters = listOf(
            string("purchase_buy__")
        )
    )

    execute {
        purchaseCheckFingerprint.let { fingerprint ->
            val method = fingerprint.method

            // 2. Find the instruction that loads the "purchase_buy__" string.
            val stringInstructionMatch = fingerprint.instructionMatches
                .firstOrNull { it.instruction.opcode.name == "CONST_STRING" }
                ?: throw PatchException(
                    "Could not find the CONST_STRING instruction in the target method."
                )

            // 3. Get the register that holds the string.
            val register = stringInstructionMatch
                .getInstruction<OneRegisterInstruction>()
                .registerA

            // 4. Insert "const/4 vX, 0x1" right after the string is loaded.
            method.addInstructions(
                stringInstructionMatch.index + 1,
                """
                    const/4 v$register, 0x1
                """
            )
        }
    }
}
