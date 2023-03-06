package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.R

class BlurWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification(appContext.getString(R.string.loading), appContext);

        return try {
            val sourceBitmap = BitmapFactory.decodeResource(appContext.resources, R.drawable.android_cupcake);
            val blurredBitmap = blurBitmap(sourceBitmap, appContext)
            val uri = writeBitmapToFile(appContext, blurredBitmap)
            makeStatusNotification(appContext.getString(R.string.blurred_ok, uri.toString()), appContext);
            Result.success()
        }catch (t: Throwable){
            makeStatusNotification(appContext.getString(R.string.blurred_error), appContext);
            Result.failure()
        }

    }

}