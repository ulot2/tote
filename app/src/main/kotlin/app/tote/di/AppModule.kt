package app.tote.di

import app.tote.feature.picker.PickerActivity
import app.tote.feature.tile.PickerLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun pickerLauncher(): PickerLauncher = PickerLauncher { context -> PickerActivity.copyIntent(context) }
}
