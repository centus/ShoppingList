package com.org.shoppinglist.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingRepository(private val dao: ShoppingDao) {

    val allSectionsWithItems: LiveData<List<SectionWithItems>> = dao.getAllSectionsWithItems()
    val uncheckedItemsCount: LiveData<Int> = dao.getUncheckedItemsCount()
    val checkedItemsCount: LiveData<Int> = dao.getCheckedItemsCount()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)


    suspend fun insertSection(section: Section): Long {
        return dao.insertSection(section)
    }

    suspend fun updateSection(section: Section) {
        dao.updateSection(section)
    }

    suspend fun deleteSection(section: Section) {
        val defaultSectionId = dao.getDefaultSectionId()
        if (defaultSectionId != null) {
            dao.moveItemsToSection(section.id, defaultSectionId)
        }
        dao.deleteSection(section)
    }

    suspend fun getAllSections(): List<Section> {
        return dao.getAllSections()
    }

    suspend fun insertItem(item: ShoppingItem): Long {
        return dao.insertItem(item)
    }

    suspend fun updateItem(item: ShoppingItem) {
        dao.updateItem(item)
    }

    suspend fun deleteItem(item: ShoppingItem) {
        dao.deleteItem(item)
    }

    suspend fun resetAllItemCheckedStates() {
        dao.resetAllItemCheckedStates()
    }

    suspend fun resetAllPlannedStates() {
        dao.resetAllPlannedStates()
    }
    
    suspend fun resetAllItemQuantities() {
        dao.resetAllItemQuantities()
    }

    suspend fun deleteAdHocItems() {
        dao.deleteAdHocItems()
    }

    // Added for full TXT import to clear all data
    suspend fun deleteAllSectionsAndItems() {
        dao.deleteAllItems()
        dao.deleteAllSections()
    }

    // The importToDefaultSection method is now removed as TXT import will be a full overwrite.

}
