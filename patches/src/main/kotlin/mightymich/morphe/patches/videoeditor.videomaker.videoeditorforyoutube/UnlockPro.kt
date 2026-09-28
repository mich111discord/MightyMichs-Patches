package mightymich.morphe.patches.videoguru

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Unlocks Pro features in Video Guru by forcing the premium check to return true."
) {
    compatibleWith(VideoGuruCompatibility.VIDEO_GURU)

    // 1. Fingerprint: locate the method that contains the string "SubscribePro".
    //    This method checks the subscription status.
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

            // 2. Find the invoke instruction that calls getBoolean for SubscribePro.
            //    We look for a call to SharedPreferences.getBoolean followed by move-result.
            var targetIndex = -1
            var targetRegister = -1

            for (i in instructions.indices) {
                val instruction = instructions[i]
                if (instruction is ReferenceInstruction) {
                    val ref = instruction.reference
                    if (ref is MethodReference &&
                        ref.name == "getBoolean" &&
                        ref.definingClass == "Landroid/content/SharedPreferences;"
                    ) {
                        // 3. The move-result instruction follows immediately after invoke.
                        if (i + 1 < instructions.size) {
                            val next = instructions[i + 1]
                            if (next.opcode.name == "MOVE_RESULT" &&
                                next is OneRegisterInstruction
                            ) {
                                targetIndex = i + 1
                                targetRegister = next.registerA
                                break
                            }
                        }
                    }
                }
            }

            if (targetIndex == -1 || targetRegister == -1) {
                throw PatchException("Could not find getBoolean/move-result for SubscribePro.")
            }

            // 4. Replace move-result with const/4 vX, 0x1 (true).
            //    This forces the premium flag to always be true.
            method.replaceInstruction(
                targetIndex,
                "const/4 v$targetRegister, 0x1"
            )
        }
    }
}
