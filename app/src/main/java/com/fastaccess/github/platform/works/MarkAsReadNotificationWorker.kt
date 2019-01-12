package com.fastaccess.github.platform.works

import android.app.Application
import androidx.work.*
import com.fastaccess.data.repository.NotificationRepositoryProvider
import com.fastaccess.domain.repository.services.NotificationService
import com.fastaccess.github.utils.EXTRA
import com.fastaccess.github.utils.EXTRA_TWO
import io.reactivex.Single
import javax.inject.Inject

/**
 * Created by Kosh on 12.01.19.
 */
class MarkAsReadNotificationWorker @Inject constructor(
    context: Application,
    private val workerParams: WorkerParameters,
    private val notificationService: NotificationService,
    private val provider: NotificationRepositoryProvider
) : RxWorker(context, workerParams) {
    override fun createWork(): Single<Result> {
        val id = workerParams.inputData.getString(EXTRA)
        val ids = workerParams.inputData.getStringArray(EXTRA_TWO)
        if (id.isNullOrEmpty() || ids.isNullOrEmpty()) {
            return Single.just(Result.failure())
        }

        return if (!id.isNullOrEmpty()) markSingleAsRead(id) else if (!ids.isNullOrEmpty()) markMultiAsRead(ids) else Single.just(Result.failure())
    }

    private fun markSingleAsRead(id: String): Single<Result> {
        return Single.fromObservable(notificationService.markAsRead(id))
            .doOnSuccess {
                if (it.code() == 205) {
                    provider.markAsRead(id)
                }
            }
            .map { if (it.code() == 205) Result.success() else Result.failure() }
            .onErrorReturnItem(Result.failure())
    }

    private fun markMultiAsRead(ids: Array<String>): Single<Result> {
        return Single.just(ids)
            .flatMap { markMultiAsRead(it) }
    }

    companion object {
        fun enqueue(id: String) {
            val workManager = WorkManager.getInstance()
            workManager.enqueueUniqueWork(id, ExistingWorkPolicy.KEEP, OneTimeWorkRequest.Builder(MarkAsReadNotificationWorker::class.java)
                .setInputData(Data.Builder()
                    .putString(EXTRA, id)
                    .build())
                .build())
        }
    }
}