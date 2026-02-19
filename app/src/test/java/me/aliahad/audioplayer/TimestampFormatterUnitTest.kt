package me.aliahad.audioplayer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// **Validates: Requirements 9.1, 9.2**
class TimestampFormatterUnitTest : FunSpec({

    context("formatTimestamp") {
        test("0ms formats to 0:00") {
            formatTimestamp(0L) shouldBe "0:00"
        }

        test("59999ms formats to 0:59 (sub-second truncated)") {
            formatTimestamp(59_999L) shouldBe "0:59"
        }

        test("60000ms formats to 1:00") {
            formatTimestamp(60_000L) shouldBe "1:00"
        }

        test("3599999ms formats to 59:59 (just under 1 hour)") {
            formatTimestamp(3_599_999L) shouldBe "59:59"
        }

        test("3600000ms formats to 1:00:00 (exactly 1 hour)") {
            formatTimestamp(3_600_000L) shouldBe "1:00:00"
        }
    }

    context("parseTimestamp") {
        test("parses 0:00 to 0ms") {
            parseTimestamp("0:00") shouldBe 0L
        }

        test("parses 0:59 to 59000ms") {
            parseTimestamp("0:59") shouldBe 59_000L
        }

        test("parses 1:00 to 60000ms") {
            parseTimestamp("1:00") shouldBe 60_000L
        }

        test("parses 59:59 to 3599000ms") {
            parseTimestamp("59:59") shouldBe 3_599_000L
        }

        test("parses 1:00:00 to 3600000ms") {
            parseTimestamp("1:00:00") shouldBe 3_600_000L
        }
    }

    context("parseTimestamp edge cases") {
        test("invalid format with single segment throws IllegalArgumentException") {
            shouldThrow<IllegalArgumentException> {
                parseTimestamp("12345")
            }
        }
    }
})
