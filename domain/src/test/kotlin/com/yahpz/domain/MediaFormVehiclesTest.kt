package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaFormVehiclesTest {
    private val me = MediaPlateOwner.Responder("er-me")

    private fun row(id: String, plate: String, responder: String? = "er-me", event: String? = null) =
        EventMediaPlateOption(id, plate, null, null, null, eventResponderId = responder, eventId = event)

    private fun media(id: String, plateIds: List<String>) = EventMedia(
        id = id, eventId = "e1", uploadedBy = "u1", uploaderName = null, treatedPlateIds = plateIds,
        caption = null, takenWhen = EventMediaTakenWhen.BEFORE_TREATMENT, storagePath = "e1/$id.jpg",
        mimeType = "image/jpeg", byteSize = 1, width = null, height = null, createdAt = "", signedUrl = null,
    )

    @Test
    fun anUnsavedFormVehicleIsOfferedForAPhoto() {
        val options = mediaVehicleOptions(
            formVehicles = listOf(TreatedPlate("12-345-67"), TreatedPlate("7654321"), TreatedPlate("12-34567")),
            saved = emptyList(),
        )
        assertEquals(listOf(formVehicleKey("1234567"), formVehicleKey("7654321")), options.map { it.id })
    }

    @Test
    fun withoutAFormTheSavedRowsAreOffered() {
        val saved = listOf(row("p1", "1234567"))
        assertEquals(saved, mediaVehicleOptions(formVehicles = null, saved = saved))
    }

    @Test
    fun anUnsavedVehicleResolvesAsPendingAndASavedOneToItsRow() {
        val saved = listOf(row("p1", "12-345-67"), row("other", "7654321", responder = "er-someone-else"))
        val resolved = resolveMediaPlateChoice(
            chosen = listOf(formVehicleKey("1234567"), formVehicleKey("7654321")),
            saved = saved,
            owner = me,
        )
        assertEquals(listOf("p1"), resolved.ids)
        assertEquals(listOf("7654321"), resolved.missing)
    }

    @Test
    fun afterTheFormSavesThePendingVehicleResolvesToTheNewRow() {
        val before = resolvePlateDigits(emptyList(), listOf("7654321"), me)
        assertEquals(listOf("7654321"), before.missing)
        val after = resolvePlateDigits(listOf(row("p9", "76-543-21")), listOf("7654321"), me)
        assertEquals(listOf("p9"), after.ids)
        assertEquals(emptyList<String>(), after.missing)
    }

    @Test
    fun shiftBornRowsAreMatchedByEvent() {
        val saved = listOf(row("s1", "1234567", responder = null, event = "e1"))
        assertEquals(listOf("s1"), resolvePlateDigits(saved, listOf("1234567"), MediaPlateOwner.Event("e1")).ids)
        assertEquals(listOf("1234567"), resolvePlateDigits(saved, listOf("1234567"), me).missing)
    }

    @Test
    fun aSavedPhotoShowsItsLinkedAndPendingVehiclesAsChipKeys() {
        val keys = mediaChoiceKeys(
            linkedIds = listOf("p1"),
            saved = listOf(row("p1", "12-345-67")),
            pendingDigits = listOf("7654321"),
            formMode = true,
        )
        assertEquals(listOf(formVehicleKey("1234567"), formVehicleKey("7654321")), keys)
        assertEquals(listOf("p1"), mediaChoiceKeys(listOf("p1"), emptyList(), emptyList(), formMode = false))
    }

    @Test
    fun aVehicleRemovedFromTheFormDropsOutOfChoicesAndPendingLinks() {
        val options = mediaVehicleOptions(listOf(TreatedPlate("1234567")), emptyList())
        assertEquals(
            listOf(formVehicleKey("1234567")),
            keepOfferedChoices(listOf(formVehicleKey("1234567"), formVehicleKey("7654321")), options),
        )
        assertEquals(
            listOf(PendingMediaLink("m1", listOf("1234567"))),
            pendingLinksInForm(mapOf("m1" to listOf("1234567", "7654321"), "m2" to listOf("7654321")), options),
        )
    }

    @Test
    fun linkedDigitsLeaveThePendingList() {
        val pending = mapOf("m1" to listOf("1234567", "7654321"), "gone" to listOf("1234567"))
        val left = prunePendingLinks(pending, listOf(media("m1", listOf("p1"))), listOf(row("p1", "1234567")))
        assertEquals(mapOf("m1" to listOf("7654321")), left)
    }

    @Test
    fun pendingLinksCountAsPhotoCoverage() {
        assertEquals(
            setOf("1111111", "7654321"),
            withPendingPhotoLinks(setOf("1111111"), listOf(PendingMediaLink("m1", listOf("76-543-21")))),
        )
        assertNull(withPendingPhotoLinks(null, listOf(PendingMediaLink("m1", listOf("7654321")))))
    }
}
