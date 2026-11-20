package com.example.pickup

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

sealed class DeleteOutcome {
    object Done : DeleteOutcome()
    /** 需要用户在系统弹窗里确认；retryAfter = Android 10，确认后要再删一次 */
    class NeedsConsent(val sender: IntentSender, val retryAfter: Boolean) : DeleteOutcome()
    object Failed : DeleteOutcome()
}

object MediaDelete {
    /** 只有相册里的真实图片（content://media/external/images/media/xxx）才能删，系统图片选择器给的临时地址不行 */
    fun isMediaStoreUri(uri: Uri): Boolean {
        val seg = uri.pathSegments
        return uri.authority == MediaStore.AUTHORITY &&
            seg.size >= 3 && seg[0] == "external" && seg[1] == "images" && seg[2] == "media"
    }

    /**
     * 删除指定的这一张图片：
     * Android 11+：由系统弹窗让用户确认后才会删；
     * Android 10：第一次会要求用户授权；
     * Android 9 及以下：直接删这一张。
     */
    fun tryDelete(context: Context, uri: Uri): DeleteOutcome {
        if (!isMediaStoreUri(uri)) return DeleteOutcome.Failed
        val resolver = context.contentResolver
        return try {
            if (Build.VERSION.SDK_INT >= 30) {
                DeleteOutcome.NeedsConsent(
                    MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender,
                    false
                )
            } else {
                if (resolver.delete(uri, null, null) > 0) DeleteOutcome.Done else DeleteOutcome.Failed
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
                DeleteOutcome.NeedsConsent(e.userAction.actionIntent.intentSender, true)
            } else {
                DeleteOutcome.Failed
            }
        } catch (e: Exception) {
            DeleteOutcome.Failed
        }
    }
}
