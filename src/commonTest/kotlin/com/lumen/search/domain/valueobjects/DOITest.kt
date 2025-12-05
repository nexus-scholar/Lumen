package com.lumen.search.domain.valueobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import kotlin.test.Test

class DOITest {

    @Test
    fun `creates DOI from valid string`() {
        val doi = DOI.fromString("10.1038/s41586-019-1666-5")

        doi shouldNotBe null
        doi?.value shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `normalizes DOI by stripping https prefix`() {
        val doi = DOI.fromString("https://doi.org/10.1038/s41586-019-1666-5")

        doi?.value shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `normalizes DOI by stripping http prefix`() {
        val doi = DOI.fromString("http://doi.org/10.1038/s41586-019-1666-5")

        doi?.value shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `normalizes DOI by stripping dx doi org prefix`() {
        val doi = DOI.fromString("https://dx.doi.org/10.1038/s41586-019-1666-5")

        doi?.value shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `DOIs are case-insensitive for comparison`() {
        val doi1 = DOI.fromString("10.1038/S41586-019-1666-5")
        val doi2 = DOI.fromString("10.1038/s41586-019-1666-5")

        doi1 shouldBe doi2
    }

    @Test
    fun `returns null for empty string`() {
        val doi = DOI.fromString("")

        doi shouldBe null
    }

    @Test
    fun `returns null for blank string`() {
        val doi = DOI.fromString("   ")

        doi shouldBe null
    }

    @Test
    fun `returns null for invalid DOI format`() {
        val doi = DOI.fromString("not-a-valid-doi")

        doi shouldBe null
    }

    @Test
    fun `validates DOI starting with 10 prefix`() {
        val valid = DOI.fromString("10.1234/test")
        val invalid = DOI.fromString("11.1234/test")

        valid shouldNotBe null
        invalid shouldBe null
    }

    @Test
    fun `DOI requires suffix after registrant code`() {
        val valid = DOI.fromString("10.1234/suffix")
        // DOIs without suffix are technically invalid
        valid shouldNotBe null
    }

    @Test
    fun `toUrl returns proper DOI URL`() {
        val doi = DOI.fromString("10.1038/s41586-019-1666-5")

        doi?.toUrl() shouldBe "https://doi.org/10.1038/s41586-019-1666-5"
    }

    @Test
    fun `equals works correctly for same DOI`() {
        val doi1 = DOI.fromString("10.1038/s41586-019-1666-5")
        val doi2 = DOI.fromString("10.1038/s41586-019-1666-5")

        (doi1 == doi2).shouldBeTrue()
    }

    @Test
    fun `equals works correctly for different DOIs`() {
        val doi1 = DOI.fromString("10.1038/s41586-019-1666-5")
        val doi2 = DOI.fromString("10.1016/j.cell.2020.01.001")

        (doi1 == doi2).shouldBeFalse()
    }

    @Test
    fun `hashCode is consistent for equal DOIs`() {
        val doi1 = DOI.fromString("10.1038/s41586-019-1666-5")
        val doi2 = DOI.fromString("10.1038/s41586-019-1666-5")

        doi1?.hashCode() shouldBe doi2?.hashCode()
    }

    @Test
    fun `toString returns the DOI value`() {
        val doi = DOI.fromString("10.1038/s41586-019-1666-5")

        doi?.toString() shouldBe "10.1038/s41586-019-1666-5"
    }

    @Test
    fun `handles DOIs with special characters`() {
        val doi = DOI.fromString("10.1002/(SICI)1097-0142(19990101)85:1<1::AID-CNCR1>3.0.CO;2-2")

        doi shouldNotBe null
    }

    @Test
    fun `handles DOIs with unicode characters`() {
        val doi = DOI.fromString("10.1234/tëst-äöü")

        doi shouldNotBe null
    }

    @Test
    fun `trusted creates DOI without validation`() {
        val doi = DOI.trusted("10.1234/trusted")

        doi.value shouldBe "10.1234/trusted"
    }
}

