package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium flag to true."
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the constructor of class Lf2/l;.
    val constructorFingerprint = Fingerprint(
        definingClass = "Lf2/l;",
        name = "<init>",
        returnType = "V"
    )

    execute {
        constructorFingerprint.let { fingerprint ->
            val method = fingerprint.method
            val instructions = method.implementation!!.instructions.toList()

            // 2. Find the instruction const/4 vX, 0x0 (initializes the premium flag to false).
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
                throw PatchException("Could not find const/4 with value 0 in constructor.")
            }

            // 3. Get the register used by that const/4 instruction.
            val const4Instruction = instructions[const4Index] as OneRegisterInstruction
            val register = const4Instruction.registerA

            // 4. Insert instructions right after the const/4 to force the premium flag to true.
            //    This sets the static field Z in class Lf2/l; to true.
            method.addInstructions(
                const4Index + 1,
                """
                    const/4 v$register, 0x1
                    sput-boolean v$register, Lf2/l;->Z:Z
                """
            )
        }
    }
}
