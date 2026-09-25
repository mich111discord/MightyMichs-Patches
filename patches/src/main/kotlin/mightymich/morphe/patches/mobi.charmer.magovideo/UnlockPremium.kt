package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium check to return true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the class that contains the string "onetime_purchase".
    //    We then look for a method inside this class that also has a const/4 vX, 0x0.
    val classFingerprint = Fingerprint(
        filters = listOf(
            string("onetime_purchase")
        )
    )

    // 2. Second fingerprint: within the found class, find a method returning boolean (Z)
    //    that contains a const/4 instruction with value 0.
    val methodFingerprint = Fingerprint(
        returnType = "Z",
        classFingerprint = classFingerprint
    )

    execute {
        methodFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val instructions = method.implementation?.instructions?.toList()
                ?: throw PatchException("Method has no implementation.")

            // 3. Find the const/4 instruction with value 0 (false).
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

            // 4. Get the register used by that const/4 instruction.
            val const4Instruction = instructions[const4Index] as OneRegisterInstruction
            val register = const4Instruction.registerA

            // 5. Replace const/4 vX, 0x0 with const/4 vX, 0x1 (true).
            method.replaceInstruction(
                const4Index,
                "const/4 v$register, 0x1"
            )
        }
    }
}
