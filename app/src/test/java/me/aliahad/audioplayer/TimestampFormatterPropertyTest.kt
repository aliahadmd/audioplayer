package me.aliahad.audioplayer

import io.kotest.core.spec.style.FunSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.forAll

// Feature: audio-timestamps, Property: Format/parse round-trip consistency
// **Validates: Requirement 9.3**
class TimestampFormatterPropertyTest : FunSpec({

    test("parseTimestamp(formatTimestamp(positionMs)) equals positionMs truncated to whole seconds") {
        forAll(Arb.long(0L..359_999_000L)) { positionMs ->
            val formatted = formatTimestamp(positionMs)
            val parsed = parseTimestamp(formatted)
            val expected = (positionMs / 1000) * 1000
            parsed == expected
        }
    }
})
