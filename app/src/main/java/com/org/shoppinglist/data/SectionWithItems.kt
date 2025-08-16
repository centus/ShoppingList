package com.org.shoppinglist.data

import androidx.room.Embedded
import androidx.room.Relation

data class SectionWithItems(
    @Embedded val section: Section,
    @Relation(
        parentColumn = "id",
        entityColumn = "sectionId"
    )
    val items: List<ShoppingItem>
)

