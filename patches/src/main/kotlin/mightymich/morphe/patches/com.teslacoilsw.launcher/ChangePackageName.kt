package mightymich.morphe.patches.com.teslacoilsw.launcher

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import org.w3c.dom.Element

private const val NOVA_PKG = "com.teslacoilsw.launcher"

private var resolvedPackage: String = NOVA_PKG

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

            // a) Nazwy klas – uczyń w pełni kwalifikowanymi
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

            // b) Uprawnienia, akcje, kategorie
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

            // c) Authorities, uprawnienia providerów, affinity
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

            // d) Sam pakiet
            root.setAttribute("package", target)
        }

        // e) Zasoby XML (targetPackage)
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
    description = "Safely changes the package name of Nova Launcher, updating both AndroidManifest.xml and DEX string references. WARNING: May cause crashes.",
    default = true
) {
    compatibleWith(NovaLauncherCompatibility.NOVA_LAUNCHER)
    dependsOn(changePackageNameResourcesPatch)

    execute {
        val target = resolvedPackage
        val oldPackage = NOVA_PKG

        // Iteracja po wszystkich klasach w DEX
        classDefs.forEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)

            mutableClass.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach
                val instructions = implementation.instructions.toList()

                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.CONST_STRING &&
                        instruction.opcode != Opcode.CONST_STRING_JUMBO
                    ) return@forEachIndexed

                    val ref = (instruction as ReferenceInstruction).reference as StringReference
                    val originalString = ref.string

                    if (originalString.contains(oldPackage)) {
                        val updatedString = originalString.replace(oldPackage, target)
                        if (updatedString != originalString) {
                            val register = (instruction as OneRegisterInstruction).registerA
                            method.replaceInstruction(
                                index,
                                "const-string v$register, \"$updatedString\""
                            )
                        }
                    }
                }
            }
        }
    }
}
