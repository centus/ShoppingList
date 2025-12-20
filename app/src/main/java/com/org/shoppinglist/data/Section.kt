package com.org.shoppinglist.data

import androidx.room.Entity
import androidx.room.Ignore // Added back
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0, 
    var name: String,   
    var orderIndex: Int,
    var isDefault: Boolean
) {
    @Ignore
    var isExpanded: Boolean = false // Added back
}
