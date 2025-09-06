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
            // Section: לא מוקצה
            val unassignedSectionId = dao.insertSection(Section(name = "לא מוקצה", orderIndex = 0, isDefault = true))
            // This section has no items by default as per your JSON.

            var sectionOrderIndex = 1 // Start next section index from 1

            // Section: ירקות ופירות
            val vegFruitId = dao.insertSection(Section(name = "ירקות ופירות", orderIndex = sectionOrderIndex++, isDefault = false))
            val vegFruitItems = listOf(
                "עגבניות", "מלפפונים", "עגבניות שרי בצבעים שונים", "תפוחי אדמה אדומים", "פטריות", "גזרים", "כרובית", "כרוב",
                "קישואים", "בצל יבש", "חסה", "חציל", "גמבה", "תפוחים ירוקים", "תפוחים פינק ליידי", "ענבים", "תותים",
                "שזיפים", "תפוזים", "בצל סגול", "לימונים", "אבטיח", "רימונים", "מלון", "בננות", "מנדרינות", "נקטרינות",
                "בטטה", "דלעת", "תרד", "אבוקדו", "תמרים אורגניים", "צימוקים", "פטרוזיליה", "כוסברה", "בזיליקום",
                "שמיר", "בצל ירוק", "מקלות סלרי"
            )
            vegFruitItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = vegFruitId, name = itemName, orderIndex = index))
            }

            // Section: מוצרי חלב
            val dairyId = dao.insertSection(Section(name = "מוצרי חלב", orderIndex = sectionOrderIndex++, isDefault = false))
            val dairyItems = listOf(
                "חלב", "חמאה", "פרומעז", "גבינה צהובה פרוסה", "גבינה צהובה פירורים", "בולגרית 24%", "פטה", "מוצרלה פרוסה",
                "גבינה לבנה", "קוטג'", "שמנת חמוצה", "שמנת מתוקה", "יוגורטים 3%", "יוגורטים עם חלבון", "פרמזן", "שמרית", "אקטימל", "ביצים"
            )
            dairyItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = dairyId, name = itemName, orderIndex = index))
            }

            // Section: בשר
            val meatId = dao.insertSection(Section(name = "בשר", orderIndex = sectionOrderIndex++, isDefault = false))
            val meatItems = listOf(
                "נקניק סלמי", "בשר לשניצלים", "המבורגרים"
            )
            meatItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = meatId, name = itemName, orderIndex = index))
            }

            // Section: מזווה
            val pantryId = dao.insertSection(Section(name = "מזווה", orderIndex = sectionOrderIndex++, isDefault = false))
            val pantryItems = listOf(
                "פריכיות", "אורז", "אורז מלא", "פתיתים", "פסטה", "שמן", "שמן זית", "מייפל", "אבקת מרק בטעם עוף",
                "אבקת מרק בטעם פטריות", "אבקת מרק בטעם בצל", "פירורי לחם", "שקדי מרק", "קפה", "תה ארל גריי", "שוקו",
                "קמח", "סוכר לבן", "סוכר חום", "מלח", "רוטב אדום לפסטה", "אבקת אפייה", "סוכר וניל", "אינסטנט פודינג וניל",
                "אבקת סוכר", "סוכריות צבעוניות לעוגה", "שוקולד מריר רגיל", "פפריקה אדומה מתוקה", "פתיבר", "חומוס",
                "רוטב סויה", "פיתות", "חלה", "קטשופ", "טחינה", "דבש", "חרדל דיז'ון", "מיונז", "חטיפים", "פופקורן",
                "עוגיות לקפה", "שוקולד נוטלה", "אצבעות קינדר", "שומשום"
            )
            pantryItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = pantryId, name = itemName, orderIndex = index))
            }

            // Section: אגוזים
            val nutsId = dao.insertSection(Section(name = "אגוזים", orderIndex = sectionOrderIndex++, isDefault = false))
            val nutsItems = listOf(
                "אגוזי פקאן", "צנוברים", "מקדמיה", "אגוזי מלך", "גרעיני חמנייה קלופים", "שקדים קלופים"
            )
            nutsItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = nutsId, name = itemName, orderIndex = index))
            }

            // Section: קפואים
            val frozenId = dao.insertSection(Section(name = "קפואים", orderIndex = sectionOrderIndex++, isDefault = false))
            val frozenItems = listOf(
                "ג'חנון", "מלאווח", "בורקסים קטנים", "פיצות קטנות", "שניצל תירס", "שעועית דקה קפואה", "אפונה קפואה",
                "ברוקולי קפוא", "שימורי חומוס קפוא", "גלידה וניל חלבית"
            )
            frozenItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = frozenId, name = itemName, orderIndex = index))
            }

            // Section: משקאות
            val drinksId = dao.insertSection(Section(name = "משקאות", orderIndex = sectionOrderIndex++, isDefault = false))
            val drinksItems = listOf(
                "סודה", "ויטמינצ'יק", "שלוקים"
            )
            drinksItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = drinksId, name = itemName, orderIndex = index))
            }

            // Section: מוצרי נקיון
            val cleaningId = dao.insertSection(Section(name = "מוצרי נקיון", orderIndex = sectionOrderIndex++, isDefault = false))
            val cleaningItems = listOf(
                "סבון ידיים", "סבון כביסה", "מרכך כביסה", "שפריצר ריח למייבש", "פנטסטיק (המרוכז)", "פנטסטיק נוזל רצפה",
                "נוזל לחלונות", "חומצה 00", "אקונומיקה", "אנטי קלאק אפור", "גליל סמרטוטים צהובים", "מגבונים",
                "מגבות נייר", "נייר טואלט", "מבריק למדיח", "מלח למדיח", "סבון למדיח", "ריח לשירותים", "סבון כלים",
                "תרסיס נגד נמלים", "חומר נגד עובש"
            )
            cleaningItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = cleaningId, name = itemName, orderIndex = index))
            }

            // Section: שימורים
            val cannedId = dao.insertSection(Section(name = "שימורים", orderIndex = sectionOrderIndex++, isDefault = false))
            val cannedItems = listOf(
                "קרם קוקוס", "רסק עגבניות", "עגבניות מרוסקות", "שימורי תירס", "שימורי זיתים", "זיתים שחורים"
            )
            cannedItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = cannedId, name = itemName, orderIndex = index))
            }

            // Section: כללי
            val generalId = dao.insertSection(Section(name = "כללי", orderIndex = sectionOrderIndex++, isDefault = false))
            val generalItems = listOf(
                "נייר כסף", "ניילון נצמד", "נייר אפיה", "שקיות סנדביץ'", "תבניות חד-פעמיות מלבניות", "חד-פעמי סכין",
                "חד-פעמי מזלג", "חד-פעמי כפית", "חד-פעמי כוסות קר", "חד-פעמי כוסות חם", "חד-פעמי קעריות",
                "חד-פעמי צלחות", "כפפות מדיום שחורות", "דאודורנט אור", "דאודורנט שירי", "אפטר שייב", "תחבושות",
                "תחתוניות", "שמפו", "מרכך", "משחת שיניים", "מנקי אוזניים"
            )
            generalItems.forEachIndexed { index, itemName ->
                dao.insertItem(ShoppingItem(sectionId = generalId, name = itemName, orderIndex = index))
            }
        }
    }
}
