package com.org.shoppinglist.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingRepository(private val shoppingDao: ShoppingDao, private val scope: CoroutineScope) {

    val allSectionsWithItems: LiveData<List<SectionWithItems>> = shoppingDao.getSectionsWithItems()
    val uncheckedItemsCount: LiveData<Int> = shoppingDao.getUncheckedItemsCount()
    val checkedItemsCount: LiveData<Int> = shoppingDao.getCheckedItemsCount()

    // Section operations
    suspend fun insertSection(section: Section): Long {
        return shoppingDao.insertSection(section)
    }

    suspend fun updateSection(section: Section) {
        shoppingDao.updateSection(section)
    }

    suspend fun deleteSection(section: Section) {
        scope.launch(Dispatchers.IO) {
            // Move items to default section before deleting
            val defaultSection = shoppingDao.getDefaultSection()
            defaultSection?.let { default ->
                shoppingDao.moveItemsToSection(section.id, default.id)
            }
            shoppingDao.deleteSection(section)
        }
    }

    // Item operations
    suspend fun insertItem(item: ShoppingItem): Long {
        return shoppingDao.insertItem(item)
    }

    suspend fun updateItem(item: ShoppingItem) {
        shoppingDao.updateItem(item)
    }

    suspend fun deleteItem(item: ShoppingItem) {
        shoppingDao.deleteItem(item)
    }

    suspend fun updateItemCheckedStatus(itemId: Long, isChecked: Boolean) {
        shoppingDao.updateItemCheckedStatus(itemId, isChecked)
    }

    suspend fun moveItemToSection(itemId: Long, newSectionId: Long) {
        val item = shoppingDao.getAllItems().value?.find { it.id == itemId }
        item?.let {
            val updatedItem = it.copy(sectionId = newSectionId)
            shoppingDao.updateItem(updatedItem)
        }
    }

    suspend fun resetAllItemCheckedStates() {
        shoppingDao.resetAllItemCheckedStates()
    }

    suspend fun deleteAdHocItems() {
        shoppingDao.deleteAdHocItems()
    }

    // Utility functions
    suspend fun getDefaultSection(): Section? {
        return shoppingDao.getDefaultSection()
    }

    suspend fun getAllSections(): List<Section> {
        return shoppingDao.getAllSectionsSync()
    }
}
