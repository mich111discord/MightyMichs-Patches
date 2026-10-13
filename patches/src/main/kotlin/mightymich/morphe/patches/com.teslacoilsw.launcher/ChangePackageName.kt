package mightymich.morphe.patches.com.teslacoilsw.launcher.ChangePackageName

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import mightymich.morphe.patches.com.teslacoilsw.launcher.NovaLauncherCompatibility
import org.w3c.dom.Element

private const val NOVA_PKG = "com.teslacoilsw.launcher"

private var resolvedPackage: String = NOVA_PKG

private val IDENTITY_SUFFIXES = listOf(
    ".permission.", ".action.", ".settings", ".preferences", ".extra."
)

private val CLASS_ATTRS = listOf(
    "android:name",
    "android:targetActivity",
    "android:backupAgent",
    "android:parentActivityName",
    "android:manageSpaceActivity",
    "android:appComponentFactory"
)

private val COMPONENT_TAGS = listOf(
    "application", "activity", "activity-alias", "service", "receiver", "provider"
)

private val IDENTITY_ATTRS = listOf(
    "android:authorities",
    "android:readPermission",
    "android:writePermission",
    "android:permission",
    "android:taskAffinity",
    "android:process",
    "android:targetPackage"
)

private val changePackageNameResourcesPatch = resourcePatch {
    val newPackage by stringOption(
        key = "packageName",
        default = "com.teslacoilsw.launcher.mightymich",
        title = "New package name",
        description = "Package name to use instead of $NOVA_PKG.",
        required = true
    ) { it != null && Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$").matches(it) }

    execute {
        val target = newPackage!!
        resolvedPackage = target

        document("AndroidManifest.xml").use { dom ->
            val root = dom.documentElement

            for (tag in COMPONENT_TAGS) {
                val nodes = root.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val e = nodes.item(i) as Element
                    for (attr in CLASS_ATTRS) {
                        val v = e.getAttribute(attr)
                        if (v.isEmpty()) continue
                        when {
                            v.startsWith(".") -> e.setAttribute(attr, NOVA_PKG + v)
                            !v.contains('.') -> e.setAttribute(attr, "$NOVA_PKG.$v")
                        }
                    }
                }
            }

            for (tag in listOf(
                "uses-permission", "uses-permission-sdk-23", "permission",
                "permission-group", "action", "category"
            )) {
                val nodes = root.getElementsByTagName(tag)
                for (i in 0 until nodes.length) {
                    val e = nodes.item(i) as Element
                    val v = e.getAttribute("android:name")
                    if (v == NOVA_PKG ||
                        v.startsWith("$NOVA_PKG.permission") ||
                        v.startsWith("$NOVA_PKG.action") ||
                        v.startsWith("$NOVA_PKG.intent")
                    ) {
                        e.setAttribute("android:name", target + v.removePrefix(NOVA_PKG))
                    }
                }
            }

            fun walk(e: Element) {
                for (attr in IDENTITY_ATTRS) {
                    val v = e.getAttribute(attr)
                    if (v.contains(NOVA_PKG)) {
                        e.setAttribute(attr, v.replace(NOVA_PKG, target))
                    }
                }
                val children = e.childNodes
                for (i in 0 until children.length) {
                    (children.item(i) as? Element)?.let(::walk)
                }
            }
            walk(root)

            root.setAttribute("package", target)
        }

        runCatching {
            get("res/xml").walkTopDown()
                .filter { it.isFile && it.extension == "xml" }
                .forEach { f ->
                    val text = f.readText()
                    if (text.contains("targetPackage=\"$NOVA_PKG\"")) {
                        f.writeText(
                            text.replace(
                                "targetPackage=\"$NOVA_PKG\"",
                                "targetPackage=\"$target\""
                            )
                        )
                    }
                }
        }
    }
}

@Suppress("unused")
val changePackageNamePatch = bytecodePatch(
    name = "Change Package Name (Experimental)",
    description = "Safely changes the package name of Nova Launcher and rewrites Nova-specific references to prevent conflicts and crashes. WARNING: May cause crashes.",
    default = false
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)
    dependsOn(changePackageNameResourcesPatch)

    execute {
        val target = resolvedPackage

        fun rewrite(s: String): String? = when {
            s == NOVA_PKG -> target
            IDENTITY_SUFFIXES.any { s.startsWith(NOVA_PKG + it) } ->
                target + s.removePrefix(NOVA_PKG)
            else -> null
        }

        classDefs.forEach { classDef ->
            val candidates = classDef.methods.filter { m ->
                m.implementation?.instructions?.any { ins ->
                    val s = ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string
                    s != null && s.startsWith(NOVA_PKG)
                } == true
            }
            if (candidates.isEmpty()) return@forEach

            val mutableClass = mutableClassDefBy(classDef)
            mutableClass.methods.forEach { method ->
                val impl = method.implementation ?: return@forEach
                impl.instructions.toList().forEachIndexed { index, ins ->
                    if (ins.opcode != Opcode.CONST_STRING &&
                        ins.opcode != Opcode.CONST_STRING_JUMBO
                    ) return@forEachIndexed
                    val str = (ins as ReferenceInstruction).reference as StringReference
                    val replacement = rewrite(str.string) ?: return@forEachIndexed
                    val reg = (ins as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "const-string v$reg, \"$replacement\"")
                }
            }
        }
    }
}
