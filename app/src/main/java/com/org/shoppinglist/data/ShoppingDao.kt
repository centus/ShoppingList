package com.org.shoppinglist.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ShoppingDao {

    // Section operations
    @Query("SELECT * FROM sections ORDER BY orderIndex ASC")
    fun getAllSections(): LiveData<List<Section>>

    @Query("SELECT * FROM sections ORDER BY orderIndex ASC")
    suspend fun getAllSectionsSync(): List<Section>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)

    @Query("SELECT * FROM sections WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSection(): Section?

    // Shopping item operations
    @Query("SELECT * FROM shopping_items ORDER BY orderIndex ASC")
    fun getAllItems(): LiveData<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE sectionId = :sectionId ORDER BY orderIndex ASC")
    fun getItemsBySection(sectionId: Long): LiveData<List<ShoppingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE id = :itemId")
    suspend fun updateItemCheckedStatus(itemId: Long, isChecked: Boolean)

    @Query("UPDATE shopping_items SET sectionId = :newSectionId WHERE sectionId = :oldSectionId")
    suspend fun moveItemsToSection(oldSectionId: Long, newSectionId: Long)

    @Query("UPDATE shopping_items SET isChecked = 0")
    suspend fun resetAllItemCheckedStates()

    @Query("DELETE FROM shopping_items WHERE isAdHoc = 1")
    suspend fun deleteAdHocItems()

    // Combined queries
    @Transaction
    @Query("SELECT * FROM sections ORDER BY orderIndex ASC")
    fun getSectionsWithItems(): LiveData<List<SectionWithItems>>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE isChecked = 0")
    fun getUncheckedItemsCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE isChecked = 1")
    fun getCheckedItemsCount(): LiveData<Int>
}
