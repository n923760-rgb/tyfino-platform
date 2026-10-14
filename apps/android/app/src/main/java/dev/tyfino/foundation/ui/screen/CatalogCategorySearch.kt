package dev.tyfino.foundation.ui.screen

import dev.tyfino.foundation.xtream.CatalogCategory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Same input ceiling as cached item search; never split a supplementary Unicode code point. */
internal fun boundCategoryPickerQuery(value: String): String =
    if (value.codePointCount(0, value.length) <= 80) value
    else value.substring(0, value.offsetByCodePoints(0, 80))

/** Called away from the UI thread on the existing bounded category snapshot, retaining provider order. */
internal suspend fun filterPickerCategories(
    categories: List<CatalogCategory>,
    query: String,
): List<CatalogCategory> {
    val term = boundCategoryPickerQuery(query).trim()
    if (term.isEmpty()) return categories
    val context = currentCoroutineContext()
    return buildList {
        categories.forEachIndexed { index, category ->
            if (index % 128 == 0) context.ensureActive()
            if (category.name.contains(term, ignoreCase = true)) add(category)
        }
        context.ensureActive()
    }
}
