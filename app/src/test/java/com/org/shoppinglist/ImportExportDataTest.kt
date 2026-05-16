package com.org.shoppinglist

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.org.shoppinglist.data.Section
import com.org.shoppinglist.data.SectionWithItems
import com.org.shoppinglist.data.ShoppingItem
import com.org.shoppinglist.data.SimpleSection
import com.org.shoppinglist.data.toExportItem
import com.org.shoppinglist.data.toExportSections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportDataTest {
    @Test
    fun importOldListWithoutImageOrLinkDefaultsToNull() {
        val legacyJson = """
            [
              {
                "name": "Test Section",
                "items": [
                  { "name": "Milk", "isPlanned": true, "quantity": 2 }
                ]
              }
            ]
        """.trimIndent()

        val typeToken = object : TypeToken<List<SimpleSection>>() {}.type
        val sections: List<SimpleSection> = Gson().fromJson(legacyJson, typeToken)
        val item = sections.first().items.first()

        assertEquals("Milk", item.name)
        assertEquals(true, item.isPlanned)
        assertEquals(2, item.quantity)
        assertNull(item.imageUri)
        assertNull(item.productLink)
    }

    @Test
    fun fullExportUsesDbIsPlanned() {
        val item = ShoppingItem(
            id = 1,
            name = "Milk",
            sectionId = 1,
            isPlanned = true,
            isChecked = true,
            quantity = 2
        )
        val exported = item.toExportItem(shortageMode = false)
        assertEquals(true, exported.isPlanned)
        assertEquals(2, exported.quantity)
    }

    @Test
    fun shortageExport_plannedUnchecked_isPlannedTrue() {
        val item = ShoppingItem(
            id = 1,
            name = "Milk",
            sectionId = 1,
            isPlanned = true,
            isChecked = false
        )
        assertTrue(item.toExportItem(shortageMode = true).isPlanned)
    }

    @Test
    fun shortageExport_plannedChecked_isPlannedFalse() {
        val item = ShoppingItem(
            id = 1,
            name = "Milk",
            sectionId = 1,
            isPlanned = true,
            isChecked = true
        )
        assertFalse(item.toExportItem(shortageMode = true).isPlanned)
    }

    @Test
    fun shortageExport_unplannedUnchecked_isPlannedFalse() {
        val item = ShoppingItem(
            id = 1,
            name = "Milk",
            sectionId = 1,
            isPlanned = false,
            isChecked = false
        )
        assertFalse(item.toExportItem(shortageMode = true).isPlanned)
    }

    @Test
    fun shortageExport_adHocUnchecked_isPlannedTrue() {
        val item = ShoppingItem(
            id = 1,
            name = "Bread",
            sectionId = 1,
            isPlanned = true,
            isChecked = false,
            isAdHoc = true
        )
        assertTrue(item.toExportItem(shortageMode = true).isPlanned)
    }

    @Test
    fun exportSections_preservesEmptySections() {
        val sections = listOf(
            SectionWithItems(
                section = Section(id = 1, name = "Full", orderIndex = 0, isDefault = false),
                items = listOf(
                    ShoppingItem(id = 1, name = "A", sectionId = 1, orderIndex = 1)
                )
            ),
            SectionWithItems(
                section = Section(id = 2, name = "Empty", orderIndex = 1, isDefault = false),
                items = emptyList()
            )
        )
        val exported = sections.toExportSections(shortageMode = false)
        assertEquals(2, exported.size)
        assertEquals("Empty", exported[1].name)
        assertTrue(exported[1].items.isEmpty())
    }

    @Test
    fun shortageExport_allChecked_allItemsUnplanned() {
        val sections = listOf(
            SectionWithItems(
                section = Section(id = 1, name = "Trip", orderIndex = 0, isDefault = false),
                items = listOf(
                    ShoppingItem(id = 1, name = "A", sectionId = 1, isPlanned = true, isChecked = true),
                    ShoppingItem(id = 2, name = "B", sectionId = 1, isPlanned = true, isChecked = true, orderIndex = 1)
                )
            )
        )
        val exported = sections.toExportSections(shortageMode = true)
        assertEquals(2, exported.first().items.size)
        assertTrue(exported.first().items.all { !it.isPlanned })
    }

    @Test
    fun shortageExport_gsonRoundTrip() {
        val sections = listOf(
            SectionWithItems(
                section = Section(id = 1, name = "Trip", orderIndex = 0, isDefault = false),
                items = listOf(
                    ShoppingItem(id = 1, name = "Milk", sectionId = 1, isPlanned = true, isChecked = false),
                    ShoppingItem(id = 2, name = "Bread", sectionId = 1, isPlanned = true, isChecked = true, orderIndex = 1)
                )
            )
        )
        val exportData = sections.toExportSections(shortageMode = true)
        val json = Gson().toJson(exportData)
        val typeToken = object : TypeToken<List<SimpleSection>>() {}.type
        val parsed: List<SimpleSection> = Gson().fromJson(json, typeToken)
        assertEquals(true, parsed.first().items[0].isPlanned)
        assertEquals(false, parsed.first().items[1].isPlanned)
    }
}
