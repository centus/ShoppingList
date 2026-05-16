package com.org.shoppinglist.data

data class SimpleSection(val name: String, val items: List<SimpleItem>)
data class SimpleItem(
    val name: String,
    val isPlanned: Boolean,
    val quantity: Int = 1,
    val imageUri: String? = null,
    val productLink: String? = null
)

fun ShoppingItem.toExportItem(shortageMode: Boolean) = SimpleItem(
    name = name,
    isPlanned = if (shortageMode) isPlanned && !isChecked else isPlanned,
    quantity = quantity,
    imageUri = imageUri,
    productLink = productLink
)

fun List<SectionWithItems>.toExportSections(shortageMode: Boolean): List<SimpleSection> =
    map { swi ->
        SimpleSection(
            swi.section.name,
            swi.items
                .sortedBy { it.orderIndex }
                .map { it.toExportItem(shortageMode) }
        )
    }