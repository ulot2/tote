package app.tote.di

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import app.tote.core.data.platform.PickerIntents
import app.tote.feature.picker.AutofillPickerActivity
import app.tote.feature.picker.PickerActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun pickerIntents(): PickerIntents = object : PickerIntents {
        override fun copyIntent(context: Context): Intent = PickerActivity.copyIntent(context)
        override fun fillIntent(context: Context, autofillId: Parcelable, urlField: Boolean, source: String?, signature: String?): Intent =
            AutofillPickerActivity.fillIntent(context, autofillId, urlField, source, signature)
    }
}
