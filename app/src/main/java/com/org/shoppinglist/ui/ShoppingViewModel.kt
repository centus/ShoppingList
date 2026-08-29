package com.org.shoppinglist.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.org.shoppinglist.data.*
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _allSectionsWithItems: LiveData<List<SectionWithItems>> = repository.allSectionsWithItems
    val uncheckedItemsCount: LiveData<Int> = repository.uncheckedItemsCount
    val checkedItemsCount: LiveData<Int> = repository.checkedItemsCount

    private val _isShoppingMode = MutableLiveData(false)
    val isShoppingMode: LiveData<Boolean> = _isShoppingMode

    private val _showPurchasedItems = MutableLiveData(false)
    val showPurchasedItems: LiveData<Boolean> = _showPurchasedItems

    // Tracks last applied mode so LiveData re-delivery (e.g. after rotation) is not treated as a mode change.
    private var lastAppliedShoppingMode: Boolean? = null

    val displayedList = MediatorLiveData<List<SectionWithItems>>()

    /**
     * True for the current [displayedList] emission when per-row item animations should be skipped
     * (mode change, show-purchased toggle, or the initial load).
     */
    var skipListAnimations: Boolean = false
        private set

    init {
        // When underlying data changes (e.g., item checked), preserve expansion state and animate check-off.
        displayedList.addSource(_allSectionsWithItems) { sections ->
            updateDisplayedList(
                sectionsFromDb = sections,
                currentActualMode = _isShoppingMode.value ?: false,
                showPurchasedItems = _showPurchasedItems.value ?: false,
                isModeChange = false,
                skipAnimations = displayedList.value == null
            )
        }
        // When shopping mode itself changes, reset expansion state and skip staggered animations.
        displayedList.addSource(_isShoppingMode) { mode ->
            val isModeChange = lastAppliedShoppingMode != null && lastAppliedShoppingMode != mode
            lastAppliedShoppingMode = mode
            updateDisplayedList(
                sectionsFromDb = _allSectionsWithItems.value,
                currentActualMode = mode,
                showPurchasedItems = _showPurchasedItems.value ?: false,
                isModeChange = isModeChange,
                skipAnimations = true
            )
        }
        displayedList.addSource(_showPurchasedItems) { showPurchased ->
            updateDisplayedList(
                sectionsFromDb = _allSectionsWithItems.value,
                currentActualMode = _isShoppingMode.value ?: false,
                showPurchasedItems = showPurchased,
                isModeChange = false,
                skipAnimations = true
            )
        }
    }

    private fun updateDisplayedList(
        sectionsFromDb: List<SectionWithItems>?,
        currentActualMode: Boolean,
        showPurchasedItems: Boolean,
        isModeChange: Boolean,
        skipAnimations: Boolean
    ) {
        skipListAnimations = skipAnimations
        val oldDisplayedSectionsMap: Map<Long, SectionWithItems> =
            displayedList.value?.associateBy { it.section.id } ?: emptyMap()
        displayedList.value = ShoppingDisplayList.build(
            sectionsFromDb = sectionsFromDb,
            isShoppingMode = currentActualMode,
            showPurchasedItems = showPurchasedItems,
            oldDisplayedSections = oldDisplayedSectionsMap,
            isModeChange = isModeChange
        )
    }

    fun toggleShoppingMode() {
        val enteringShopping = !(_isShoppingMode.value ?: false)
        if (!enteringShopping && _showPurchasedItems.value == true) {
            _showPurchasedItems.value = false
        }
        _isShoppingMode.value = enteringShopping
    }

    fun toggleShowPurchasedItems() {
        if (_isShoppingMode.value != true) {
            return
        }
        _showPurchasedItems.value = !(_showPurchasedItems.value ?: false)
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
                val updatedSection = existingSectionWithItems.section.copyDisplayState(
                    isExpanded = newExpandedState
                )
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

    fun updateItemImage(item: ShoppingItem, newImageUri: String?) {
        viewModelScope.launch {
            val updatedItem = item.copy(imageUri = newImageUri)
            repository.updateItem(updatedItem)
        }
    }

    fun updateItemDetails(item: ShoppingItem, imageUri: String?, productLink: String?) {
        viewModelScope.launch {
            val updatedItem = item.copy(imageUri = imageUri, productLink = productLink)
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

    fun setItemPlanned(item: ShoppingItem, isPlanned: Boolean) {
        viewModelScope.launch {
            val updatedItem = item.copy(isPlanned = isPlanned)
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
            _showPurchasedItems.value = false
            _isShoppingMode.value = false
        }
    }

    suspend fun buildFullListExportData(): List<SimpleSection> {
        return repository.getAllSectionsWithItemsOnce().toExportSections(shortageMode = false)
    }

    suspend fun buildShortageListExportData(): List<SimpleSection> {
        return repository.getAllSectionsWithItemsOnce().toExportSections(shortageMode = true)
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
                        quantity = simpleItem.quantity,   // Use quantity from SimpleItem (defaults to 1 if not present)
                        orderIndex = itemOrder++,
                        isAdHoc = false,                  // Default for imported items
                        imageUri = simpleItem.imageUri,
                        productLink = simpleItem.productLink
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
