package app.tote.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType

@Database(
    entities = [EntryEntity::class, ProfileEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class ToteDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun profileDao(): ProfileDao

    companion object {
        const val NAME = "tote.db"
    }
}

/** Enums are stored by name so reordering enum constants never corrupts data. */
internal class EnumConverters {
    @TypeConverter fun typeToString(v: EntryType) = v.name
    @TypeConverter fun stringToType(v: String) = enumValueOrNull<EntryType>(v) ?: EntryType.SHORT_TEXT
    @TypeConverter fun categoryToString(v: Category) = v.name
    @TypeConverter fun stringToCategory(v: String) = enumValueOrNull<Category>(v) ?: Category.ANSWERS
    @TypeConverter fun iconToString(v: EntryIcon) = v.name
    @TypeConverter fun stringToIcon(v: String) = enumValueOrNull<EntryIcon>(v) ?: EntryIcon.TEXT

    private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
