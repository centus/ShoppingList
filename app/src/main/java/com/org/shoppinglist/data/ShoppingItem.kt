package com.org.shoppinglist.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = Section::class,
        parentColumns = ["id"],
        childColumns = ["sectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sectionId"])]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String,
    var sectionId: Long,
    var quantity: Int = 1,
    var isChecked: Boolean = false,
    var isAdHoc: Boolean = false, // Item added quickly during shopping mode
    var orderIndex: Int = 0,      // For ordering within a section
    var isPlanned: Boolean = true, // New field: true if selected in planning mode for the shopping trip
    var imageUri: String? = null,  // Optional image to help identify the item
    var productLink: String? = null // Optional product link
)

