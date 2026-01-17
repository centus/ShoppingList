package com.org.shoppinglist

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.org.shoppinglist.data.SimpleSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
