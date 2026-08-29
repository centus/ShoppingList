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

    // Display-only shopping progress, computed from all planned items (including hidden purchased).
    @Ignore
    var shoppingCheckedCount: Int = 0

    @Ignore
    var shoppingPlannedCount: Int = 0

    fun copyDisplayState(
        isExpanded: Boolean = this.isExpanded,
        shoppingCheckedCount: Int = this.shoppingCheckedCount,
        shoppingPlannedCount: Int = this.shoppingPlannedCount
    ): Section {
        return Section(
            id = id,
            name = name,
            orderIndex = orderIndex,
            isDefault = isDefault
        ).apply {
            this.isExpanded = isExpanded
            this.shoppingCheckedCount = shoppingCheckedCount
            this.shoppingPlannedCount = shoppingPlannedCount
        }
    }
}
