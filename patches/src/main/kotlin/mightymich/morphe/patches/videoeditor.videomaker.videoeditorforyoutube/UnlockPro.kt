package mightymich.morphe.patches.videoguru

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro Features",
    description = "Unlocks Pro features in Video Guru by forcing the premium check method a()Z to return true."
) {
    compatibleWith(VideoGuruCompatibility.VIDEO_GURU)

    val aFingerprint = Fingerprint(
        name = "a",
        returnType = "Z",
        parameters = listOf()
    )

    execute {
        aFingerprint.let { fingerprint ->
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
