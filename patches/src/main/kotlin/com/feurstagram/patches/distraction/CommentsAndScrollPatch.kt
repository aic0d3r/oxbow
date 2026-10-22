package com.feurstagram.patches.distraction

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.feurstagram.patches.shared.CONFIG_CLASS
import com.feurstagram.patches.shared.Constants.COMPATIBILITY_INSTAGRAM
import com.feurstagram.patches.shared.Constants.EXTENSION
import com.feurstagram.patches.shared.hasStringEndingWith
import com.feurstagram.patches.shared.returnEarlyIf

private const val VIEW_PAGER2 = "Landroidx/viewpager2/widget/ViewPager2;"

// The only creator of the comment sheet; every comment button, "View all comments" and deep
// link ends up here.
internal object CommentSheetFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("launchCommentThreadMvvmBottomsheetFragment"),
)

// The feed UFI state (like/comment/share row): its toString names the comment fields.
internal object FeedUfiStateToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf(", isCommentsEnabled=", ", shouldShowCommentCountInUfi="),
)

// Reels' comment button state: logs "ReelsCommentButton", then picks hidden / button / count.
internal object ReelsCommentStateFingerprint : Fingerprint(
    strings = listOf("ReelsCommentButton"),
    custom = { method, _ -> method.hasStringEndingWith("UfiUseCase_getCommentButtonAndCount") },
)

internal object ReelPagerSetupFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ -> method.hasStringEndingWith("ClipsViewPagerImpl_setupView") },
)

internal object ReelPagerEnableScrollingFingerprint : Fingerprint(
    returnType = "V",
    custom = { method, _ -> method.hasStringEndingWith("ClipsViewPagerImpl_enableScrolling") },
)

@Suppress("unused")
val commentsAndScrollPatch = bytecodePatch(
    name = "Comments and endless scroll",
    description = "Optional comment blocking (no comment button, no comment sheet) and no swiping " +
        "to the next reel in reel viewers. Runtime toggles.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)

    extendWith(EXTENSION)

    execute {
        // --- Comments ---
        CommentSheetFingerprint.method.returnEarlyIf("isCommentsBlocked")

        // Feed comment button: clear isCommentsEnabled / shouldShowCommentCountInUfi on the
        // state the UFI is drawn from, in the one method that builds it.
        val stateType = FeedUfiStateToStringFingerprint.classDef.type
        val toString = FeedUfiStateToStringFingerprint.method.instructions.toList()
        // toString appends "<label>" then the field's value, loaded into a register earlier on:
        // trace that register back through moves to its iget-boolean.
        fun fieldAfter(label: String): FieldReference {
            val at = toString.indexOfFirst { it.getReference<StringReference>()?.string == label }
            val append = (at until toString.size).firstOrNull {
                toString[it].getReference<MethodReference>()?.let { r -> r.name == "append" && r.parameterTypes.firstOrNull()?.toString() == "Z" } == true
            } ?: throw PatchException("UFI state: no append after \"$label\"")
            var reg = (toString[append] as FiveRegisterInstruction).registerD
            for (i in append - 1 downTo 0) {
                val insn = toString[i]
                if ((insn as? OneRegisterInstruction)?.registerA != reg) continue
                when (insn.opcode) {
                    Opcode.MOVE, Opcode.MOVE_FROM16, Opcode.MOVE_16 -> reg = (insn as TwoRegisterInstruction).registerB
                    Opcode.IGET_BOOLEAN -> return insn.getReference<FieldReference>()!!
                    else -> break
                }
            }
            throw PatchException("UFI state: no field behind \"$label\"")
        }
        val commentsEnabled = fieldAfter(", isCommentsEnabled=")
        val commentCount = fieldAfter(", shouldShowCommentCountInUfi=")
        var builders = 0
        classDefForEach { classDef ->
            classDef.methods.filter { m ->
                m.returnType == stateType && m.implementation?.instructions?.any {
                    it.opcode == Opcode.NEW_INSTANCE && it.getReference<TypeReference>()?.type == stateType
                } == true
            }.forEach { found ->
                val method = mutableClassDefBy(classDef).methods.first {
                    it.name == found.name && it.parameterTypes == found.parameterTypes && it.returnType == found.returnType
                }
                method.instructions.filter { it.opcode == Opcode.RETURN_OBJECT }.reversed().forEach { ret ->
                    val reg = (ret as OneRegisterInstruction).registerA
                    val tmp = if (reg == 0) 1 else 0 // everything but the returned register is dead here
                    if (reg > 15) throw PatchException("UFI state builder: return register v$reg")
                    method.addInstructionsWithLabels(
                        ret.location.index,
                        """
                            invoke-static { }, $CONFIG_CLASS->isCommentsBlocked()Z
                            move-result v$tmp
                            if-eqz v$tmp, :keep
                            const/4 v$tmp, 0x0
                            iput-boolean v$tmp, v$reg, $commentsEnabled
                            iput-boolean v$tmp, v$reg, $commentCount
                        """,
                        ExternalLabel("keep", ret),
                    )
                }
                builders++
            }
        }
        if (builders == 0) throw PatchException("UFI state builder not found")

        // Reels comment button: right after the log, Instagram loads its "hidden" state into a
        // register (sget of the state class's own singleton) and every path then merges on a
        // `const/4`. Re-load the hidden state there when comments are blocked.
        ReelsCommentStateFingerprint.method.apply {
            val insns = instructions.toList()
            val log = insns.indexOfFirst { it.getReference<StringReference>()?.string == "ReelsCommentButton" }
            val hiddenAt = (log until insns.size).first { insns[it].opcode == Opcode.SGET_OBJECT }
            val hidden = insns[hiddenAt].getReference<FieldReference>()!!
            if (hidden.type != hidden.definingClass) throw PatchException("Reels comment state: not a singleton load")
            val stateReg = (insns[hiddenAt] as OneRegisterInstruction).registerA
            val mergeAt = hiddenAt + 1
            val merge = insns[mergeAt]
            if (merge.opcode != Opcode.CONST_4) throw PatchException("Reels comment state: merge point moved")
            val tmp = (merge as OneRegisterInstruction).registerA
            addInstructionsWithLabels(
                mergeAt + 1,
                """
                    invoke-static { }, $CONFIG_CLASS->isCommentsBlocked()Z
                    move-result v$tmp
                    if-eqz v$tmp, :oxbow_keep
                    sget-object v$stateReg, $hidden
                    :oxbow_keep
                    const/4 v$tmp, ${(merge as com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction).narrowLiteral}
                """,
            )
        }

        // --- Endless reels: the reel viewer's pager starts with swiping off... ---
        ReelPagerSetupFingerprint.method.apply {
            val store = instructions.firstOrNull {
                it.opcode == Opcode.IPUT_OBJECT && it.getReference<FieldReference>()?.type == VIEW_PAGER2
            } ?: throw PatchException("Reel pager setup: ViewPager2 store not found")
            val pager = (store as TwoRegisterInstruction).registerA
            addInstructions(
                store.location.index + 1,
                "invoke-static/range { v$pager .. v$pager }, Lcom/feurstagram/extension/Scroll;->onReelPager(Ljava/lang/Object;)V",
            )
        }
        // ...and Instagram's own "enable scrolling" calls are ignored.
        ReelPagerEnableScrollingFingerprint.method.returnEarlyIf("isEndlessScrollBlocked")
    }
}
