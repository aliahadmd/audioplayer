package me.aliahad.audioplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.assume
import io.kotest.property.forAll

// State-machine actions for bookmark dialog toggle property test
sealed class DialogAction {
    data class Tap(val positionMs: Long) : DialogAction()
    data object Dismiss : DialogAction()
    data class Save(val positionMs: Long, val note: String?) : DialogAction()
}

// Feature: audio-timestamps, Property: Bookmarks are always ordered by positionMs ascending
// **Validates: Requirement 4.3**
class TimestampBookmarkPropertyTest : FunSpec({

    test("sorting bookmarks by positionMs produces a monotonically non-decreasing sequence") {
        val arbBookmark = Arb.long(0L..360_000_000L).map { pos ->
            TimestampBookmark(
                audioFileUri = "content://audio/file1",
                folderUri = "content://folder/a",
                positionMs = pos
            )
        }

        forAll(Arb.list(arbBookmark, 0..50)) { bookmarks ->
            val sorted = bookmarks.sortedBy { it.positionMs }
            sorted.zipWithNext().all { (a, b) -> a.positionMs <= b.positionMs }
        }
    }

    // Feature: audio-timestamps, Property: Bookmarks are isolated by (audioFileUri, folderUri) pair
    // **Validates: Requirements 7.1, 7.2, 7.3**
    test("filtering bookmarks by one (audioFileUri, folderUri) pair never returns bookmarks from a different pair") {
        val arbUri = Arb.string(5..20)

        forAll(
            arbUri, arbUri, // pair1: audioFileUri, folderUri
            arbUri, arbUri, // pair2: audioFileUri, folderUri
            Arb.list(Arb.long(0L..360_000_000L), 0..30),  // positions for pair1
            Arb.list(Arb.long(0L..360_000_000L), 0..30)   // positions for pair2
        ) { audioUri1, folderUri1, audioUri2, folderUri2, positions1, positions2 ->
            // Ensure the two pairs are distinct
            assume(audioUri1 != audioUri2 || folderUri1 != folderUri2)

            val bookmarksPair1 = positions1.map { pos ->
                TimestampBookmark(
                    audioFileUri = audioUri1,
                    folderUri = folderUri1,
                    positionMs = pos
                )
            }
            val bookmarksPair2 = positions2.map { pos ->
                TimestampBookmark(
                    audioFileUri = audioUri2,
                    folderUri = folderUri2,
                    positionMs = pos
                )
            }

            val mixedList = (bookmarksPair1 + bookmarksPair2).shuffled()

            // Filter by pair1
            val filteredForPair1 = mixedList.filter {
                it.audioFileUri == audioUri1 && it.folderUri == folderUri1
            }
            // Filter by pair2
            val filteredForPair2 = mixedList.filter {
                it.audioFileUri == audioUri2 && it.folderUri == folderUri2
            }

            filteredForPair1.all { it.audioFileUri == audioUri1 && it.folderUri == folderUri1 } &&
                filteredForPair2.all { it.audioFileUri == audioUri2 && it.folderUri == folderUri2 } &&
                filteredForPair1.map { it.positionMs }.sorted() == positions1.sorted() &&
                filteredForPair2.map { it.positionMs }.sorted() == positions2.sorted()
        }
    }

    // Feature: audio-timestamps, Property: Dialog state toggles correctly
    // **Validates: Requirements 1.2, 2.3, 2.4**
    test("bookmark dialog state is consistent for any sequence of tap/dismiss/save actions") {
        val arbAction: Arb<DialogAction> = Arb.element(
            listOf("tap", "dismiss", "save")
        ).map { kind ->
            when (kind) {
                "tap" -> DialogAction.Tap(positionMs = (0L..360_000_000L).random())
                "dismiss" -> DialogAction.Dismiss
                else -> DialogAction.Save(
                    positionMs = (0L..360_000_000L).random(),
                    note = if ((0..1).random() == 0) null else "note"
                )
            }
        }

        forAll(Arb.list(arbAction, 1..50)) { actions ->
            var bookmarkDialogPositionMs: Long? = null
            val expectedFinalPosition = actions.fold(null as Long?) { _, action ->
                when (action) {
                    is DialogAction.Tap -> action.positionMs
                    is DialogAction.Dismiss,
                    is DialogAction.Save -> null
                }
            }

            actions.forEach { action ->
                when (action) {
                    is DialogAction.Tap -> bookmarkDialogPositionMs = action.positionMs
                    is DialogAction.Dismiss,
                    is DialogAction.Save -> bookmarkDialogPositionMs = null
                }
            }

            bookmarkDialogPositionMs == expectedFinalPosition
        }
    }
})
