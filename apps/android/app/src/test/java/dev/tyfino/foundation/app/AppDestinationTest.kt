package dev.tyfino.foundation.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = AppDestination.entries.map(AppDestination::route)

        assertEquals(routes.size, routes.distinct().size)
    }
}
