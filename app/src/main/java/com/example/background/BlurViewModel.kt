/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanUpWorker
import com.example.background.workers.SaveImageToFileWorker
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation


class BlurViewModel(application: Application) : ViewModel() {

    private val workManager = WorkManager.getInstance(application)

    private var imageUri: Uri? = null

    var outputUri: Uri? = null
    var outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        imageUri = getImageUri(application.applicationContext)
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }
    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        val cleanUpWorkerRequest = OneTimeWorkRequestBuilder<CleanUpWorker>()
            .build()

        val blurWorkerRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData(createInputDataForUri())
            .build()

        /**
         * When the device is not charging, it should suspend SaveImageToFileWorker,
         * executing it only after that you plug it in.
         */
        val saveConstraints = Constraints.Builder().setRequiresCharging(true).build()

        val saveImageToFileWorkerRequest = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(saveConstraints)
            .addTag(TAG_OUTPUT)
            .build()

        var chain = workManager.beginUniqueWork(
            IMAGE_MANIPULATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            cleanUpWorkerRequest)

        chain = chain.then(blurWorkerRequest)

        for(i in 1 until blurLevel){
            val extraBlurWorkerRequest = OneTimeWorkRequestBuilder<BlurWorker>()
                .build()
            chain = chain.then(extraBlurWorkerRequest)
        }

        chain = chain.then(saveImageToFileWorkerRequest)

        chain.enqueue()

    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    internal fun cancelWorks(){
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    private fun createInputDataForUri(): Data{
        val builder = Data.Builder()
        this.imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }
}
