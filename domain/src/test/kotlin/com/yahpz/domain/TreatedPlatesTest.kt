package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TreatedPlatesTest {
    @Test
    fun commitFormats7DigitsWithHyphensAndAppends() {
        val result = commitTreatedPlate(pending = "1234567", plates = emptyList())
        val ok = result as? CommitTreatedPlateResult.Ok ?: return fail("expected ok")
        assertEquals("12-345-67", ok.plate.plateNumber)
        assertNull(ok.plate.model)
        assertNull(ok.plate.color)
        assertEquals(listOf(TreatedPlate(plateNumber = "12-345-67", model = null, color = null, leftWhere = null)), ok.plates)
    }

    @Test
    fun commitFormats8DigitsWithHyphens() {
        val result = commitTreatedPlate(pending = "71386301", plates = emptyList())
        val ok = result as? CommitTreatedPlateResult.Ok ?: return fail("expected ok")
        assertEquals("713-86-301", ok.plate.plateNumber)
    }

    @Test
    fun commitRejects6Digits() {
        val result = commitTreatedPlate(pending = "123456", plates = emptyList())
        val error = result as? CommitTreatedPlateResult.Error ?: return fail("expected error")
        assertEquals(TREATED_PLATE_LENGTH_ERROR, error.message)
    }

    @Test
    fun commitRejectsDuplicateByDigits() {
        val existing = listOf(TreatedPlate(plateNumber = "12-345-67", model = null, color = null, leftWhere = null))
        val result = commitTreatedPlate(pending = "1234567", plates = existing)
        val error = result as? CommitTreatedPlateResult.Error ?: return fail("expected error")
        assertEquals(TREATED_PLATE_DUPLICATE_ERROR, error.message)
    }

    @Test
    fun leftoverIgnoredOnDraft() {
        assertNull(leftoverTreatedPlateError(pending = "123", mode = FillMode.DRAFT))
    }

    @Test
    fun leftoverErrorsDigitsOnComplete() {
        assertEquals(
            TREATED_PLATE_LEFTOVER_ERROR,
            leftoverTreatedPlateError(pending = "123", mode = FillMode.COMPLETE),
        )
    }

    @Test
    fun leftoverAllowsEmptyPendingOnComplete() {
        assertNull(leftoverTreatedPlateError(pending = "", mode = FillMode.COMPLETE))
    }

    @Test
    fun captionJoinsModelAndColor() {
        assertEquals("REXTON · שחור", treatedPlateCaption(model = "REXTON", color = "שחור"))
    }

    @Test
    fun captionShowsSingleSideWhenOtherMissing() {
        assertEquals("REXTON", treatedPlateCaption(model = "REXTON", color = null))
        assertEquals("שחור", treatedPlateCaption(model = null, color = "שחור"))
        assertNull(treatedPlateCaption(model = null, color = null))
    }

    @Test
    fun removeDropsByDigitMatch() {
        val plates = listOf(
            TreatedPlate(plateNumber = "12-345-67", model = null, color = null, leftWhere = null),
            TreatedPlate(plateNumber = "713-86-301", model = "REXTON", color = "שחור", leftWhere = null),
        )
        assertEquals(listOf(plates[1]), removeTreatedPlate(plates, plateDigitsKey = "1234567"))
    }

    @Test
    fun applyLookupSetsManufacturerAndLogoSlug() {
        val plates = listOf(TreatedPlate(plateNumber = "713-86-301"))
        val next = applyTreatedPlateLookup(
            plates,
            plateDigitsKey = "71386301",
            hit = PlateLookupHit(model = "REXTON", color = "שחור", manufacturer = "סאנגיונג ד.קור"),
        )
        assertEquals(
            TreatedPlate(
                plateNumber = "713-86-301",
                model = "REXTON",
                color = "שחור",
                manufacturer = "סאנגיונג ד.קור",
                logoSlug = "ssangyong",
            ),
            next.single(),
        )
    }

    @Test
    fun mapRowsOrdersBySortOrder() {
        val rows = listOf(
            TreatedPlateRowInput(
                plateNumber = "713-86-301",
                model = "REXTON",
                color = "שחור",
                leftWhere = null,
                manufacturer = "סאנגיונג ד.קור",
                logoSlug = "ssangyong",
                sortOrder = 1,
            ),
            TreatedPlateRowInput(plateNumber = "12-345-67", model = null, color = null, leftWhere = null, sortOrder = 0),
            TreatedPlateRowInput(plateNumber = "  ", model = null, color = null, sortOrder = 2),
        )
        assertEquals(
            listOf(
                TreatedPlate(plateNumber = "12-345-67", model = null, color = null, leftWhere = null),
                TreatedPlate(
                    plateNumber = "713-86-301",
                    model = "REXTON",
                    color = "שחור",
                    leftWhere = null,
                    manufacturer = "סאנגיונג ד.קור",
                    logoSlug = "ssangyong",
                ),
            ),
            mapTreatedPlateRows(rows),
        )
    }

    @Test
    fun hebrewErrorStringsMatchWeb() {
        assertEquals("יש להזין 7 או 8 ספרות.", TREATED_PLATE_LENGTH_ERROR)
        assertEquals("מספר זה כבר נוסף.", TREATED_PLATE_DUPLICATE_ERROR)
        assertEquals("יש ללחוץ הוספה לשמירת המספר", TREATED_PLATE_LEFTOVER_ERROR)
        assertTrue(true)
    }
}
