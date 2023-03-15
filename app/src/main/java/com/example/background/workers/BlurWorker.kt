package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R

class BlurWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification(appContext.getString(R.string.loading), appContext);
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        return try {
            if(TextUtils.isEmpty(resourceUri)) {
                throw IllegalArgumentException("Invalid input URI")
            }

            val sourceBitmap = BitmapFactory.decodeStream(appContext.contentResolver.openInputStream(
                Uri.parse(resourceUri)))

            val blurredBitmap = blurBitmap(sourceBitmap, appContext)

            val outputUri = writeBitmapToFile(appContext, blurredBitmap)

            makeStatusNotification(appContext.getString(R.string.blurred_ok, outputUri.toString()), appContext);

            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            Result.success(outputData)
        }catch (t: Throwable){
            makeStatusNotification(appContext.getString(R.string.blurred_error), appContext);
            Result.failure()
        }

    }

}