package com.org.shoppinglist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sectionId: Long,
    val isChecked: Boolean = false,
    val isAdHoc: Boolean = false,
    val orderIndex: Int = 0
)

