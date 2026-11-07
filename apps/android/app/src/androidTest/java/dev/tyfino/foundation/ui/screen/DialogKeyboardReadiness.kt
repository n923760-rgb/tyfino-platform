package dev.tyfino.foundation.ui.screen

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertTrue

/** A test precondition, never a focus request, key retry or replacement navigation path. */
internal fun awaitKeyboardDialogWindow() {
    val keyboardDialog = object : TypeSafeMatcher<Root>() {
        override fun describeTo(description: Description) {
            description.appendText("dialog with native window/child focus outside touch mode")
        }

        override fun matchesSafely(root: Root): Boolean {
            if (!isDialog().matches(root)) return false
            val view = root.decorView
            val target = view.findFocus()
            return view.hasWindowFocus() && !view.isInTouchMode &&
                target != null && target.isFocused && target.hasWindowFocus()
        }
    }
    // Espresso's bounded root wait must fail if native readiness never arrives.
    onView(isRoot()).inRoot(keyboardDialog).check { view, error ->
        if (error != null) throw error
        requireNotNull(view)
        assertTrue(view.hasWindowFocus())
        assertTrue(!view.isInTouchMode)
        assertTrue(view.findFocus()?.isFocused == true)
    }
}
