package com.org.shoppinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ShoppingItem::class, Section::class], version = 1, exportSchema = false)
abstract class ShoppingDatabase : RoomDatabase() {
    
    abstract fun shoppingDao(): ShoppingDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShoppingDatabase? = null
        
        fun getDatabase(context: Context, scope: CoroutineScope): ShoppingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingDatabase::class.java,
                    "shopping_database"
                )
                .addCallback(ShoppingDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private class ShoppingDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.shoppingDao())
                }
            }
        }
        
        suspend fun populateDatabase(dao: ShoppingDao) {
            // Create default sections
            val defaultSection = Section(name = "לא מוקצה", orderIndex = 0, isDefault = true)
            val defaultSectionId = dao.insertSection(defaultSection)
            
            val sections = listOf(
                Section(name = "ירקות ופירות", orderIndex = 1, isDefault = false),
                Section(name = "מוצרי חלב", orderIndex = 2, isDefault = false),
                Section(name = "בשר", orderIndex = 3, isDefault = false),
                Section(name = "מזווה", orderIndex = 4, isDefault = false),
                Section(name = "קפואים", orderIndex = 5, isDefault = false),
                Section(name = "משקאות", orderIndex = 6, isDefault = false),
                Section(name = "מוצרי ניקיון", orderIndex = 7, isDefault = false),
                Section(name = "כללי", orderIndex = 8, isDefault = false)
            )
            
            val sectionIds = mutableListOf<Long>()
            sectionIds.add(defaultSectionId)
            
            for (section in sections) {
                val id = dao.insertSection(section)
                sectionIds.add(id)
            }
            
            // Add sample items
            val sampleItems = listOf(
                ShoppingItem(name = "תפוחים", sectionId = sectionIds[1], orderIndex = 0),
                ShoppingItem(name = "בננות", sectionId = sectionIds[1], orderIndex = 1),
                ShoppingItem(name = "חלב", sectionId = sectionIds[2], orderIndex = 0),
                ShoppingItem(name = "גבינה", sectionId = sectionIds[2], orderIndex = 1),
                ShoppingItem(name = "חזה עוף", sectionId = sectionIds[3], orderIndex = 0),
                ShoppingItem(name = "בשר טחון", sectionId = sectionIds[3], orderIndex = 1),
                ShoppingItem(name = "פסטה", sectionId = sectionIds[4], orderIndex = 0),
                ShoppingItem(name = "אורז", sectionId = sectionIds[4], orderIndex = 1),
                ShoppingItem(name = "גלידה", sectionId = sectionIds[5], orderIndex = 0),
                ShoppingItem(name = "ירקות קפואים", sectionId = sectionIds[5], orderIndex = 1),
                ShoppingItem(name = "קפה", sectionId = sectionIds[6], orderIndex = 0),
                ShoppingItem(name = "תה", sectionId = sectionIds[6], orderIndex = 1),
                ShoppingItem(name = "מגבות נייר", sectionId = sectionIds[7], orderIndex = 0),
                ShoppingItem(name = "נוזל כלים", sectionId = sectionIds[7], orderIndex = 1)
            )
            
            for (item in sampleItems) {
                dao.insertItem(item)
            }
        }
    }
}

