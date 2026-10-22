package com.feurstagram.patches.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.StringReference

const val CONFIG_CLASS = "Lcom/feurstagram/extension/Config;"

/** True when the method loads a string ending with [suffix] (purge-log prefixes change per release). */
fun Method.hasStringEndingWith(suffix: String): Boolean =
    implementation?.instructions?.any { it.getReference<StringReference>()?.string?.endsWith(suffix) == true } == true

/** Fingerprint on a method that loads a string ending with [suffix]. */
fun stringSuffixFingerprint(suffix: String, returnType: String? = null) = Fingerprint(
    returnType = returnType,
    custom = { method, _ -> method.hasStringEndingWith(suffix) },
)

/** Free local registers at method entry (parameters sit in the highest registers). */
fun Method.freeLocals(): Int {
    val params = parameterTypes.sumOf { if (it.toString() == "J" || it.toString() == "D") 2 else 1 }
    val self = if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    return (implementation?.registerCount ?: 0) - params - self
}

/**
 * At method entry: `if (Config.<toggle>()) return <value>;`. Uses v0 only, which is free at
 * entry whenever the method has a local register.
 */
fun MutableMethod.returnEarlyIf(toggle: String, returnSmali: String = "return-void") {
    if (freeLocals() < 1) throw PatchException("$name has no free register for the $toggle hook")
    addInstructionsWithLabels(
        0,
        """
            invoke-static { }, $CONFIG_CLASS->$toggle()Z
            move-result v0
            if-eqz v0, :instagram
            $returnSmali
        """,
        ExternalLabel("instagram", getInstruction(0)),
    )
}
