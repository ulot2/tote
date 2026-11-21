package app.tote.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.tote.core.data.db.EntryDao
import app.tote.core.data.db.ProfileDao
import app.tote.core.data.db.ToteDatabase
import app.tote.core.data.repo.Clock
import app.tote.core.data.repo.EntryRepository
import app.tote.core.data.repo.RoomEntryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): ToteDatabase =
        Room.databaseBuilder(context, ToteDatabase::class.java, ToteDatabase.NAME).build()

    @Provides fun entryDao(db: ToteDatabase): EntryDao = db.entryDao()
    @Provides fun profileDao(db: ToteDatabase): ProfileDao = db.profileDao()

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides fun clock(): Clock = Clock.System
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds abstract fun entryRepository(impl: RoomEntryRepository): EntryRepository
}
