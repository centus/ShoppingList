package com.org.shoppinglist.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.org.shoppinglist.data.*
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _allSectionsWithItems: LiveData<List<SectionWithItems>> = repository.allSectionsWithItems
    val uncheckedItemsCount: LiveData<Int> = repository.uncheckedItemsCount
    val checkedItemsCount: LiveData<Int> = repository.checkedItemsCount

    private val _isShoppingMode = MutableLiveData(false)
    val isShoppingMode: LiveData<Boolean> = _isShoppingMode

    val displayedList = MediatorLiveData<List<SectionWithItems>>()

    init {
        // When underlying data changes (e.g., item checked), preserve expansion state
        displayedList.addSource(_allSectionsWithItems) { sections ->
            updateDisplayedList(sections, _isShoppingMode.value ?: false, isModeChange = false)
        }
        // When shopping mode itself changes, reset expansion state
        displayedList.addSource(_isShoppingMode) { mode ->
            updateDisplayedList(_allSectionsWithItems.value, mode, isModeChange = true)
        }
    }

    // Modified updateDisplayedList method
    private fun updateDisplayedList(
        sectionsFromDb: List<SectionWithItems>?,
        currentActualMode: Boolean,
        isModeChange: Boolean // New parameter
    ) {
        // Capture the state of the currently displayed list BEFORE this update
        val oldDisplayedSectionsMap: Map<Long, SectionWithItems> =
            displayedList.value?.associateBy { it.section.id } ?: emptyMap()

        if (sectionsFromDb == null) {
            displayedList.value = emptyList()
            return
        }

        val newProcessedList = sectionsFromDb.mapNotNull { sectionFromDbWithItems ->
            val sectionFromDb = sectionFromDbWithItems.section

            // Determine the correct isExpanded state
            val determinedIsExpandedState = if (isModeChange) {
                false // If it's a mode change, always collapse sections
            } else {
                // Otherwise, preserve the existing expansion state from the old displayed list,
                // or default to false if the section is new or wasn't found.
                oldDisplayedSectionsMap[sectionFromDb.id]?.section?.isExpanded ?: false
            }

            // Create a new Section object, applying the determined isExpanded state
            // (Manual construction as isExpanded is @Ignore)
            val finalSectionStateForDisplay = Section(
                id = sectionFromDb.id,
                name = sectionFromDb.name,
                orderIndex = sectionFromDb.orderIndex,
                isDefault = sectionFromDb.isDefault
            ).apply { // apply extension function to set the @Ignore property
                isExpanded = determinedIsExpandedState
            }

            // Filter items based on the current mode
            val itemsToFilter = sectionFromDbWithItems.items
            val itemsToDisplay = if (currentActualMode) { // Shopping mode
                itemsToFilter.filter { it.isPlanned }
            } else { // Planning mode
                itemsToFilter // Show all items
            }

            // Only include the section if it's Planning mode,
            // OR if it's Shopping mode AND has items to display.
            if (!currentActualMode || itemsToDisplay.isNotEmpty()) {
                // Create SectionWithItems with a new ArrayList for its items, for DiffUtil robustness
                SectionWithItems(section = finalSectionStateForDisplay, items = ArrayList(itemsToDisplay))
            } else {
                null // Exclude section in Shopping mode if it has no planned items
            }
        }
        displayedList.value = newProcessedList
    }
    fun toggleShoppingMode() {
        _isShoppingMode.value = !(_isShoppingMode.value ?: false)
    }

    fun setShoppingMode(enabled: Boolean) {
        _isShoppingMode.value = enabled
    }

    // Section operations
    fun addSection(name: String) {
        viewModelScope.launch {
            val sections = repository.getAllSections() // Suspend function call
            val newOrderIndex = sections.maxOfOrNull { it.orderIndex }?.plus(1) ?: 0
            val newSection = Section(name = name, orderIndex = newOrderIndex, isDefault = false)
            repository.insertSection(newSection)
        }
    }

    fun updateSectionName(section: Section, newName: String) {
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

    suspend fun getAllSectionsNonLiveData(): List<Section> {
        return repository.getAllSections()
    }

    fun updateSectionExpansionState(sectionToUpdate: Section, newExpandedState: Boolean) {
        val currentList = displayedList.value ?: return // Get the current list

        val newList = currentList.map { existingSectionWithItems ->
            if (existingSectionWithItems.section.id == sectionToUpdate.id) {
                // isExpanded is an @Ignore var, so it's not in copy().
                // Manually create a new Section, copying constructor properties, then set isExpanded.
                val originalSection = existingSectionWithItems.section
                val updatedSection = Section(
                    id = originalSection.id,
                    name = originalSection.name,
                    orderIndex = originalSection.orderIndex,
                    isDefault = originalSection.isDefault
                )
                updatedSection.isExpanded = newExpandedState // Set the @Ignore var manually

                // Create a new SectionWithItems object.
                SectionWithItems(section = updatedSection, items = ArrayList(existingSectionWithItems.items))
            } else {
                // For all other sections, return the existing SectionWithItems instance.
                existingSectionWithItems
            }
        }
        // Post the new list.
        displayedList.postValue(newList)
    }

    // Item operations
    fun addItem(name: String, sectionId: Long, isAdHoc: Boolean = false) {
        viewModelScope.launch {
            val currentDisplayedItems = displayedList.value?.find { it.section.id == sectionId }?.items ?: emptyList()
            val newOrderIndex = currentDisplayedItems.maxOfOrNull { it.orderIndex }?.plus(1) ?: 0

            val newItem = ShoppingItem(
                name = name,
                sectionId = sectionId,
                isAdHoc = isAdHoc,
                orderIndex = newOrderIndex
            )
            repository.insertItem(newItem)
        }
    }

    fun updateItemName(item: ShoppingItem, newName: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(name = newName)
            repository.updateItem(updatedItem)
        }
    }
    
    fun updateItemQuantity(item: ShoppingItem, newQuantity: Int) {
        viewModelScope.launch {
            val updatedItem = item.copy(quantity = newQuantity)
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

    fun toggleItemPlanned(item: ShoppingItem) {
        viewModelScope.launch {
            val updatedItem = item.copy(isPlanned = !item.isPlanned)
            repository.updateItem(updatedItem)
        }
    }

    fun moveItemToSection(item: ShoppingItem, newSectionId: Long) {
        viewModelScope.launch {
            val updatedItem = item.copy(sectionId = newSectionId)
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
            repository.resetAllPlannedStates()
            repository.resetAllItemQuantities()
            repository.deleteAdHocItems()
            _isShoppingMode.value = false
        }
    }


    fun importShoppingListData(importedData: List<SimpleSection>) {
        viewModelScope.launch {
            // Clear existing data first
            repository.deleteAllSectionsAndItems()

            // Optional: Reset UI states if they are not automatically cleared by data wipe
            // repository.resetAllItemCheckedStates()
            // repository.resetAllPlannedStates()
            // repository.deleteAdHocItems()
            // _isShoppingMode.value = false // Consider if this should be reset

            var sectionOrder = 0
            for (simpleSection in importedData) {
                // Assuming Section constructor takes name, orderIndex, isDefault
                val section = Section(
                    name = simpleSection.name,
                    orderIndex = sectionOrder++,
                    isDefault = false // Imported sections are generally not the aefault
                )
                val sectionId = repository.insertSection(section) // Make sure insertSection returns the ID

                var itemOrder = 0
                for (simpleItem in simpleSection.items) {
                    // Assuming ShoppingItem constructor takes sectionId, name, isPlanned, isChecked, orderIndex
                    val item = ShoppingItem(
                        sectionId = sectionId,
                        name = simpleItem.name,
                        isPlanned = simpleItem.isPlanned, // <<< Key change: Use isPlanned from SimpleItem
                        isChecked = false,                // Default for imported items
                        orderIndex = itemOrder++,
                        isAdHoc = false                   // Default for imported items
                        // Ensure all necessary ShoppingItem fields are covered
                    )
                    repository.insertItem(item)
                }
            }
            Log.d("ShoppingViewModel", "Import of shopping list data completed with 'isPlanned' status.")
            // You might need to trigger a refresh of _allSectionsWithItems if it's not automatic
            // or ensure displayedList updates correctly.
        }
    }

    // importTextToDefaultSection is now removed as TXT import uses importShoppingListData

    fun getAvailableSections(): LiveData<List<Section>> {
        return _allSectionsWithItems.map { sectionsWithItems ->
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
