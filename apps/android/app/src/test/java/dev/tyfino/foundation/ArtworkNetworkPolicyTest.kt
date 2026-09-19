package dev.tyfino.foundation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkNetworkPolicyTest {
    @Test
    fun acceptsUnknownOrBoundedContentLength() {
        assertTrue(ArtworkNetworkPolicy.acceptsContentLength(-1L))
        assertTrue(ArtworkNetworkPolicy.acceptsContentLength(ArtworkNetworkPolicy.MAX_RESPONSE_BYTES))
    }

    @Test
    fun rejectsDeclaredResponseAboveLimit() {
        assertFalse(ArtworkNetworkPolicy.acceptsContentLength(ArtworkNetworkPolicy.MAX_RESPONSE_BYTES + 1L))
    }
}
