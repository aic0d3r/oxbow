package com.feurstagram.patches.privacy

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.feurstagram.patches.shared.Constants.COMPATIBILITY_INSTAGRAM
import com.feurstagram.patches.shared.Constants.EXTENSION
import com.feurstagram.patches.shared.returnEarlyIf

// Story views. The pending-seen state class builds the media/seen request...
internal object PendingStorySeenStateFingerprint : Fingerprint(
    strings = listOf("media/seen/?reel=%s&live_vod=0"),
)

// ...and its store sends it, in the one void method taking that state.
internal object PendingStorySeenStoreFingerprint : Fingerprint(
    strings = listOf("PendingReelSeenStateStore.deserializeFromDisk"),
)

// What was viewed and when, serialized into the reels_tray request body.
internal object StoriesViewInfoFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/Map;"),
    strings = listOf("reel_media_owner_id", "reel_media_creation_seen_at"),
)

// DM read receipts: enqueues the send_thread_seen_marker mutation (and only that).
internal object DmThreadSeenFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("mark_thread_seen-"),
    custom = { method, _ -> AccessFlags.STATIC.isSet(method.accessFlags) },
)

// Typing indicator: the one sender of the MQTT "indicate_activity" command.
internal object TypingIndicatorFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("indicate_activity"),
)

@Suppress("unused")
val ghostModePatch = bytecodePatch(
    name = "Ghost mode",
    description = "Optional: watch stories without being listed as a viewer, read DMs without " +
        "sending read receipts, type without showing \"typing...\". Each is a runtime toggle.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_INSTAGRAM)

    extendWith(EXTENSION)

    execute {
        // Stories: never even build the request (the network block in Block.java is the backstop).
        val stateType = PendingStorySeenStateFingerprint.classDef.type
        val send = PendingStorySeenStoreFingerprint.classDef.methods.singleOrNull {
            it.returnType == "V" && it.parameterTypes.map(CharSequence::toString) == listOf(stateType)
        } ?: throw PatchException("Story seen store: no single send(state) method")
        mutableClassDefBy(PendingStorySeenStoreFingerprint.classDef).methods
            .first { it.name == send.name && it.parameterTypes == send.parameterTypes }
            .returnEarlyIf("isStorySeenHidden")
        StoriesViewInfoFingerprint.method.returnEarlyIf("isStorySeenHidden", "const-string v0, \"[]\"\nreturn-object v0")

        DmThreadSeenFingerprint.method.returnEarlyIf("isDmSeenHidden")
        TypingIndicatorFingerprint.method.returnEarlyIf("isTypingHidden")
    }
}
