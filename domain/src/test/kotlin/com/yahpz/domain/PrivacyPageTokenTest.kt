package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPageTokenTest {
    @Test
    fun `matches the locked web test vector`() {
        val token = createPrivacyPageToken(TEST_SECRET, TEST_NOW)
        assertEquals(TEST_VECTOR, token)
        assertTrue(verifyPrivacyPageToken(TEST_SECRET, token, TEST_NOW))
    }

    @Test
    fun `rejects expired future or tampered tokens`() {
        val token = createPrivacyPageToken(TEST_SECRET, TEST_NOW)
        assertFalse(verifyPrivacyPageToken(TEST_SECRET, token, TEST_NOW + PRIVACY_TOKEN_TTL_SEC + 61))
        assertFalse(verifyPrivacyPageToken(TEST_SECRET, token, TEST_NOW - PRIVACY_TOKEN_TTL_SEC - 61))
        assertFalse(verifyPrivacyPageToken(TEST_SECRET, token.dropLast(1) + "0", TEST_NOW))
        assertFalse(verifyPrivacyPageToken("other-secret", token, TEST_NOW))
        assertFalse(verifyPrivacyPageToken(TEST_SECRET, "", TEST_NOW))
    }

    @Test
    fun `builds the in-app privacy url with t`() {
        assertEquals(
            "https://yahpz.com/privacy?t=$TEST_VECTOR",
            buildPrivacyPolicyUrl("https://yahpz.com", TEST_VECTOR),
        )
    }

    companion object {
        private const val TEST_SECRET = "test-privacy-secret"
        private const val TEST_NOW = 1_700_000_000L
        private const val TEST_VECTOR =
            "1700000900.1b55aad767119a9b8b62ab8bd7ea29c13774c7f37a979ab966f7375a4abafa02"
    }
}
