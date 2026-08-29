package com.org.shoppinglist

import com.org.shoppinglist.data.Section
import com.org.shoppinglist.data.SectionWithItems
import com.org.shoppinglist.data.ShoppingItem
import com.org.shoppinglist.ui.ShoppingDisplayList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingDisplayListTest {

    @Test
    fun planningMode_includesAllItemsIncludingUnplanned() {
        val sections = listOf(
            sectionWithItems(
                id = 1,
                name = "Dairy",
                items = listOf(
                    item(id = 1, name = "Milk", sectionId = 1, planned = true, checked = false),
                    item(id = 2, name = "Butter", sectionId = 1, planned = false, checked = false, order = 1)
                )
            )
        )

        val result = ShoppingDisplayList.build(
            sectionsFromDb = sections,
            isShoppingMode = false,
            showPurchasedItems = false,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )

        assertEquals(1, result.size)
        assertEquals(listOf("Milk", "Butter"), result[0].items.map { it.name })
    }

    @Test
    fun shoppingHidePurchased_showsOnlyPlannedUnchecked_andOmitsCompletedSections() {
        val sections = listOf(
            sectionWithItems(
                id = 1,
                name = "Produce",
                items = listOf(
                    item(id = 1, name = "Apples", sectionId = 1, planned = true, checked = false),
                    item(id = 2, name = "Bananas", sectionId = 1, planned = true, checked = true, order = 1),
                    item(id = 3, name = "Kale", sectionId = 1, planned = false, checked = false, order = 2)
                )
            ),
            sectionWithItems(
                id = 2,
                name = "Dairy",
                items = listOf(
                    item(id = 4, name = "Milk", sectionId = 2, planned = true, checked = true)
                )
            )
        )

        val result = ShoppingDisplayList.build(
            sectionsFromDb = sections,
            isShoppingMode = true,
            showPurchasedItems = false,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )

        assertEquals(1, result.size)
        assertEquals("Produce", result[0].section.name)
        assertEquals(listOf("Apples"), result[0].items.map { it.name })
        assertEquals(1, result[0].section.shoppingCheckedCount)
        assertEquals(2, result[0].section.shoppingPlannedCount)
    }

    @Test
    fun shoppingShowPurchased_includesCheckedPlannedItems_andKeepsHeaderCounts() {
        val sections = listOf(
            sectionWithItems(
                id = 1,
                name = "Produce",
                items = listOf(
                    item(id = 1, name = "Apples", sectionId = 1, planned = true, checked = false),
                    item(id = 2, name = "Bananas", sectionId = 1, planned = true, checked = true, order = 1),
                    item(id = 3, name = "Kale", sectionId = 1, planned = false, checked = false, order = 2)
                )
            ),
            sectionWithItems(
                id = 2,
                name = "Dairy",
                items = listOf(
                    item(id = 4, name = "Milk", sectionId = 2, planned = true, checked = true)
                )
            )
        )

        val result = ShoppingDisplayList.build(
            sectionsFromDb = sections,
            isShoppingMode = true,
            showPurchasedItems = true,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )

        assertEquals(2, result.size)
        assertEquals(listOf("Apples", "Bananas"), result[0].items.map { it.name })
        assertEquals(1, result[0].section.shoppingCheckedCount)
        assertEquals(2, result[0].section.shoppingPlannedCount)
        assertEquals(listOf("Milk"), result[1].items.map { it.name })
        assertEquals(1, result[1].section.shoppingCheckedCount)
        assertEquals(1, result[1].section.shoppingPlannedCount)
    }

    @Test
    fun shoppingMode_neverIncludesUnplannedItems() {
        val sections = listOf(
            sectionWithItems(
                id = 1,
                name = "Pantry",
                items = listOf(
                    item(id = 1, name = "Rice", sectionId = 1, planned = false, checked = false)
                )
            )
        )

        val hidden = ShoppingDisplayList.build(
            sectionsFromDb = sections,
            isShoppingMode = true,
            showPurchasedItems = false,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )
        val shown = ShoppingDisplayList.build(
            sectionsFromDb = sections,
            isShoppingMode = true,
            showPurchasedItems = true,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )

        assertTrue(hidden.isEmpty())
        assertTrue(shown.isEmpty())
    }

    @Test
    fun preservesExpansionUnlessModeChange() {
        val section = sectionWithItems(
            id = 1,
            name = "Produce",
            items = listOf(item(id = 1, name = "Apples", sectionId = 1))
        )
        val previouslyExpanded = Section(
            id = 1,
            name = "Produce",
            orderIndex = 0,
            isDefault = false
        ).apply { isExpanded = true }
        val oldMap = mapOf(
            1L to SectionWithItems(previouslyExpanded, section.items)
        )

        val preserved = ShoppingDisplayList.build(
            sectionsFromDb = listOf(section),
            isShoppingMode = true,
            showPurchasedItems = false,
            oldDisplayedSections = oldMap,
            isModeChange = false
        )
        val reset = ShoppingDisplayList.build(
            sectionsFromDb = listOf(section),
            isShoppingMode = true,
            showPurchasedItems = false,
            oldDisplayedSections = oldMap,
            isModeChange = true
        )

        assertTrue(preserved.single().section.isExpanded)
        assertTrue(!reset.single().section.isExpanded)
    }

    @Test
    fun nullInput_returnsEmptyList() {
        val result = ShoppingDisplayList.build(
            sectionsFromDb = null,
            isShoppingMode = true,
            showPurchasedItems = false,
            oldDisplayedSections = emptyMap(),
            isModeChange = false
        )
        assertTrue(result.isEmpty())
    }

    private fun sectionWithItems(
        id: Long,
        name: String,
        items: List<ShoppingItem>
    ): SectionWithItems {
        return SectionWithItems(
            section = Section(id = id, name = name, orderIndex = id.toInt(), isDefault = false),
            items = items
        )
    }

    private fun item(
        id: Long,
        name: String,
        sectionId: Long,
        planned: Boolean = true,
        checked: Boolean = false,
        order: Int = 0
    ): ShoppingItem {
        return ShoppingItem(
            id = id,
            name = name,
            sectionId = sectionId,
            isPlanned = planned,
            isChecked = checked,
            orderIndex = order
        )
    }
}
