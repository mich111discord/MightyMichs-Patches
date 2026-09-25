package mightymich.morphe.patches.magovideo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium Features",
    description = "Unlocks premium features in MagoVideo by forcing the premium check method to return true. "
) {
    compatibleWith(MagoVideoCompatibility.MAGO_VIDEO)

    // 1. Fingerprint: locate the method h0(String, String)Z inside class Lf2/l;.
    //    We use customFingerprint to check both the class type and the method name.
    val h0Fingerprint = Fingerprint(
        returnType = "Z",
        parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
        customFingerprint = { methodDef, classDef ->
            methodDef.name == "h0" && classDef.type == "Lf2/l;"
        }
    )

    execute {
        h0Fingerprint.let { fingerprint ->
            val method = fingerprint.method

            // 2. Insert instructions at the very beginning of the method:
            //      const/4 v0, 0x1  -> load 1 (true) into register v0
            //      return v0        -> return true immediately
            //    This forces the premium check to always succeed.
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}
