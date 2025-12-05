package com.lumen.search.domain.valueobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import kotlin.test.Test
import kotlin.test.assertFailsWith

class YearRangeTest {

    @Test
    fun `creates valid year range`() {
        val range = YearRange(2020, 2024)

        range.start shouldBe 2020
        range.end shouldBe 2024
    }

    @Test
    fun `single year range is valid`() {
        val range = YearRange(2024, 2024)

        range.start shouldBe 2024
        range.end shouldBe 2024
    }

    @Test
    fun `rejects inverted range`() {
        assertFailsWith<IllegalArgumentException> {
            YearRange(2024, 2020)
        }
    }

    @Test
    fun `contains returns true for year in range`() {
        val range = YearRange(2020, 2024)

        (2020 in range).shouldBeTrue()
        (2022 in range).shouldBeTrue()
        (2024 in range).shouldBeTrue()
    }

    @Test
    fun `contains returns false for year outside range`() {
        val range = YearRange(2020, 2024)

        (2019 in range).shouldBeFalse()
        (2025 in range).shouldBeFalse()
    }

    @Test
    fun `toIntRange returns correct range`() {
        val range = YearRange(2020, 2024)
        val intRange = range.toIntRange()

        intRange shouldBe (2020..2024)
        intRange.count() shouldBe 5  // 2020, 2021, 2022, 2023, 2024
    }

    @Test
    fun `toIntRange of single year has count 1`() {
        val range = YearRange(2024, 2024)
        val intRange = range.toIntRange()

        intRange.count() shouldBe 1
    }

    @Test
    fun `rejects years before 1900`() {
        assertFailsWith<IllegalArgumentException> {
            YearRange(1899, 2024)
        }
    }

    @Test
    fun `rejects years after 2100`() {
        assertFailsWith<IllegalArgumentException> {
            YearRange(2020, 2101)
        }
    }

    @Test
    fun `toString returns readable format for range`() {
        val range = YearRange(2020, 2024)

        range.toString() shouldBe "2020-2024"
    }

    @Test
    fun `toString returns single year for same start and end`() {
        val range = YearRange(2024, 2024)

        range.toString() shouldBe "2024"
    }

    @Test
    fun `equals works correctly`() {
        val range1 = YearRange(2020, 2024)
        val range2 = YearRange(2020, 2024)
        val range3 = YearRange(2019, 2024)

        (range1 == range2).shouldBeTrue()
        (range1 == range3).shouldBeFalse()
    }

    @Test
    fun `hashCode is consistent for equal ranges`() {
        val range1 = YearRange(2020, 2024)
        val range2 = YearRange(2020, 2024)

        range1.hashCode() shouldBe range2.hashCode()
    }

    @Test
    fun `lastYears creates correct range`() {
        val range = YearRange.lastYears(5)

        // Should have 5 years including current
        range.toIntRange().count() shouldBe 5
    }

    @Test
    fun `lastYears rejects non-positive n`() {
        assertFailsWith<IllegalArgumentException> {
            YearRange.lastYears(0)
        }
        assertFailsWith<IllegalArgumentException> {
            YearRange.lastYears(-1)
        }
    }

    @Test
    fun `singleYear creates correct range`() {
        val range = YearRange.singleYear(2024)

        range.start shouldBe 2024
        range.end shouldBe 2024
    }
}

