package com.example.pickup

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

object Gallery {
    /** 读取最近的图片，返回顺序是“旧 → 新”，也就是最新的图片在列表最后（界面最下方） */
    fun load(context: Context, limit: Int = 600): List<Uri> {
        val result = ArrayList<Uri>()
        val base = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        try {
            context.contentResolver.query(
                base,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext() && result.size < limit) {
                    result += ContentUris.withAppendedId(base, c.getLong(idI))
                }
            }
        } catch (e: Exception) {
            // 没权限或读取失败时返回空列表，界面会提示改用系统相册
        }
        result.reverse()
        return result
    }
}
