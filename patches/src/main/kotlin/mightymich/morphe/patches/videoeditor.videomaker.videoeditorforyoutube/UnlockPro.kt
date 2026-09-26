package mightymich.morphe.patches.videoguru

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patcher.patch.AppTarget
@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Unlocks Pro features in Video Guru by forcing the premium check to return true."
) {
    // Declare compatibility with the target app.
    compatibleWith(VideoGuruCompatibility.VIDEO_GURU)

    // 1. Fingerprint: locate the method that contains the string "SubscribePro".
    //    This method is responsible for checking the subscription status.
    val subscribeProFingerprint = Fingerprint(
        filters = listOf(
            string("SubscribePro")
        )
    )

    execute {
        subscribeProFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val instructions = method.implementation?.instructions?.toList()
                ?: throw PatchException("Method has no implementation.")

            // 2. Find the const/4 instruction with value 0 (false).
            //    This flag decides the premium status.
            var const4Index = -1
            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction.opcode.name == "CONST_4" &&
                    instruction is NarrowLiteralInstruction &&
                    instruction.narrowLiteral == 0
                ) {
                    const4Index = i
                    break
                }
            }

            if (const4Index == -1) {
                throw PatchException("Could not find const/4 with value 0 in the target method.")
            }

            // 3. Get the register used by that const/4 instruction.
            val const4Instruction = instructions[const4Index] as OneRegisterInstruction
            val register = const4Instruction.registerA

            // 4. Replace const/4 vX, 0x0 with const/4 vX, 0x1 (true).
            //    This forces the method to always return true.
            method.replaceInstruction(
                const4Index,
                "const/4 v$register, 0x1"
            )
        }
    }
}
