package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium check to return true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the method h0(String, String)Z inside class Lf2/l;.
    //    This is the method that checks the premium status.
    val h0Fingerprint = Fingerprint(
        definingClass = "Lf2/l;",
        name = "h0",
        returnType = "Z",
        parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;")
    )

    execute {
        h0Fingerprint.let { fingerprint ->
            val method = fingerprint.method
            val instructions = method.implementation!!.instructions.toList()

            // 2. Find the const/4 instruction with value 0 (false) in the method.
            //    This is the instruction that initializes the premium flag to false.
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
                throw PatchException("Could not find const/4 with value 0 in method h0.")
            }

            // 3. Get the register used by that const/4 instruction.
            val const4Instruction = instructions[const4Index] as OneRegisterInstruction
            val register = const4Instruction.registerA

            // 4. Replace const/4 vX, 0x0 with const/4 vX, 0x1 (true).
            //    This forces the premium check to always succeed.
            method.replaceInstruction(
                const4Index,
                "const/4 v$register, 0x1"
            )
        }
    }
}
