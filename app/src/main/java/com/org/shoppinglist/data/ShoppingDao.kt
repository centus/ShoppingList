package com.org.shoppinglist.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ShoppingDao {

    // Section operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)

    @Query("SELECT * FROM sections ORDER BY orderIndex ASC")
    fun getAllSectionsWithItems(): LiveData<List<SectionWithItems>>

    @Query("SELECT * FROM sections ORDER BY orderIndex ASC")
    suspend fun getAllSections(): List<Section>

    @Query("SELECT * FROM sections WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSection(): Section?

    @Query("DELETE FROM sections") // Added for full TXT import
    suspend fun deleteAllSections()

    // Item operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE sectionId = :sectionId")
    suspend fun deleteItemsBySectionId(sectionId: Long)

    @Query("DELETE FROM shopping_items") // Added for full TXT import
    suspend fun deleteAllItems()

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): ShoppingItem?

    // Combined operations
    @Transaction
    @Query("SELECT * FROM sections WHERE id = :sectionId")
    suspend fun getSectionWithItems(sectionId: Long): SectionWithItems?

    // Full list operations / Reset operations
    @Query("UPDATE shopping_items SET isChecked = 0")
    suspend fun resetAllItemCheckedStates()

    @Query("UPDATE shopping_items SET isPlanned = 0 WHERE isAdHoc = 0")
    suspend fun resetAllPlannedStates()

    @Query("DELETE FROM shopping_items WHERE isAdHoc = 1")
    suspend fun deleteAdHocItems()

    // Statistics
    @Query("SELECT COUNT(*) FROM shopping_items WHERE isChecked = 0 AND isPlanned = 1")
    fun getUncheckedItemsCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE isChecked = 1 AND isPlanned = 1")
    fun getCheckedItemsCount(): LiveData<Int>
}
