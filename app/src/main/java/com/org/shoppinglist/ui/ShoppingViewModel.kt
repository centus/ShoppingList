package com.org.shoppinglist.ui

import android.util.Log
import androidx.lifecycle.*
import com.org.shoppinglist.data.*
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    val allSectionsWithItems: LiveData<List<SectionWithItems>> = repository.allSectionsWithItems
    val uncheckedItemsCount: LiveData<Int> = repository.uncheckedItemsCount
    val checkedItemsCount: LiveData<Int> = repository.checkedItemsCount

    private val _isShoppingMode = MutableLiveData(false)
    val isShoppingMode: LiveData<Boolean> = _isShoppingMode

    fun toggleShoppingMode() {
        _isShoppingMode.value = !(_isShoppingMode.value ?: false)
    }

    fun setShoppingMode(enabled: Boolean) {
        _isShoppingMode.value = enabled
    }

    // Section operations
    fun addSection(name: String) {
        viewModelScope.launch {
            val sections = repository.getAllSections() // This should ideally be from a LiveData or a suspend fun
            val newOrderIndex = sections.maxOfOrNull { it.orderIndex }?.plus(1) ?: 0
            val newSection = Section(name = name, orderIndex = newOrderIndex)
            repository.insertSection(newSection)
        }
    }

    fun updateSection(section: Section, newName: String) {
        viewModelScope.launch {
            val updatedSection = section.copy(name = newName)
            repository.updateSection(updatedSection)
        }
    }

    fun deleteSection(section: Section) {
        viewModelScope.launch {
            repository.deleteSection(section)
        }
    }

    // Item operations
    fun addItem(name: String, sectionId: Long, isAdHoc: Boolean = false) {
        viewModelScope.launch {
            val items = repository.allSectionsWithItems.value 
            val sectionItems = items?.find { it.section.id == sectionId }?.items ?: emptyList()
            val newOrderIndex = sectionItems.maxOfOrNull { it.orderIndex }?.plus(1) ?: 0

            val newItem = ShoppingItem(
                name = name,
                sectionId = sectionId,
                isAdHoc = isAdHoc,
                orderIndex = newOrderIndex
            )
            repository.insertItem(newItem)
        }
    }


    fun updateItem(item: ShoppingItem, newName: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(name = newName)
            repository.updateItem(updatedItem)
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun toggleItemChecked(item: ShoppingItem, isChecked: Boolean) { 
        viewModelScope.launch {
            val updatedItem = item.copy(isChecked = isChecked)
            repository.updateItem(updatedItem)
        }
    }

    fun moveItemToSection(item: ShoppingItem, newSectionId: Long) {
        viewModelScope.launch {
            Log.d("ShoppingViewModel", "Attempting to move item '''${item.name}''' (id: ${item.id}) from section ${item.sectionId} to new section $newSectionId")
            val updatedItem = item.copy(sectionId = newSectionId)
            Log.d("ShoppingViewModel", "Updated item for database: $updatedItem")
            repository.updateItem(updatedItem)
        }
    }

    fun resetAllItemCheckedStates() {
        viewModelScope.launch {
            repository.resetAllItemCheckedStates()
        }
    }

    fun performFullShoppingReset() {
        viewModelScope.launch {
            repository.resetAllItemCheckedStates()
            repository.deleteAdHocItems()
            _isShoppingMode.value = false
        }
    }

    fun importShoppingListData(importedData: List<SimpleSection>) {
        viewModelScope.launch {
            performFullShoppingReset() 
            _isShoppingMode.value = false 

            var sectionOrder = 0
            for (simpleSection in importedData) {
                val section = Section(name = simpleSection.name, orderIndex = sectionOrder++)
                val sectionId = repository.insertSection(section)

                var itemOrder = 0
                for (simpleItem in simpleSection.items) {
                    val item = ShoppingItem(
                        name = simpleItem.name,
                        sectionId = sectionId,
                        orderIndex = itemOrder++,
                        isAdHoc = false,
                        isChecked = false 
                    )
                    repository.insertItem(item)
                }
            }
            Log.d("ShoppingViewModel", "Import of shopping list data completed.")
        }
    }

    fun getAvailableSections(): LiveData<List<Section>> {
        return repository.allSectionsWithItems.map { sectionsWithItems ->
            sectionsWithItems.map { it.section }
        }
    }
}

class ShoppingViewModelFactory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
