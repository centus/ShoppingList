package com.org.shoppinglist.ui

import com.org.shoppinglist.data.Section
import com.org.shoppinglist.data.SectionWithItems

object ShoppingDisplayList {

    fun build(
        sectionsFromDb: List<SectionWithItems>?,
        isShoppingMode: Boolean,
        showPurchasedItems: Boolean,
        oldDisplayedSections: Map<Long, SectionWithItems>,
        isModeChange: Boolean
    ): List<SectionWithItems> {
        if (sectionsFromDb == null) {
            return emptyList()
        }

        return sectionsFromDb.mapNotNull { sectionFromDbWithItems ->
            val sectionFromDb = sectionFromDbWithItems.section
            val determinedIsExpandedState = if (isModeChange) {
                false
            } else {
                oldDisplayedSections[sectionFromDb.id]?.section?.isExpanded ?: false
            }

            val itemsToFilter = sectionFromDbWithItems.items
            val plannedItems = itemsToFilter.filter { it.isPlanned }
            val shoppingCheckedCount = plannedItems.count { it.isChecked }
            val shoppingPlannedCount = plannedItems.size

            val itemsToDisplay = if (isShoppingMode) {
                if (showPurchasedItems) {
                    plannedItems
                } else {
                    plannedItems.filter { !it.isChecked }
                }
            } else {
                itemsToFilter
            }

            if (!isShoppingMode || itemsToDisplay.isNotEmpty()) {
                val displaySection = Section(
                    id = sectionFromDb.id,
                    name = sectionFromDb.name,
                    orderIndex = sectionFromDb.orderIndex,
                    isDefault = sectionFromDb.isDefault
                ).apply {
                    isExpanded = determinedIsExpandedState
                    this.shoppingCheckedCount = shoppingCheckedCount
                    this.shoppingPlannedCount = shoppingPlannedCount
                }
                SectionWithItems(section = displaySection, items = ArrayList(itemsToDisplay))
            } else {
                null
            }
        }
    }
}
