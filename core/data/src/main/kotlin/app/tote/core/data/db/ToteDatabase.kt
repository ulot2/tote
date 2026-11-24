package app.tote.core.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import app.tote.core.model.Category
import app.tote.core.model.EntryIcon
import app.tote.core.model.EntryType

@Database(
    entities = [EntryEntity::class, ProfileEntity::class, LearnedMappingEntity::class],
    version = 2,
    exportSchema = true,
    // v2 adds learned_mappings. Adding a table is something Room migrates on its own.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(EnumConverters::class)
abstract class ToteDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun profileDao(): ProfileDao
    abstract fun learnedDao(): LearnedDao

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
