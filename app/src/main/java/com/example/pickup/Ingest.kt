package com.example.pickup

import android.content.Context
import java.util.UUID

object Ingest {
    private val lock = Any()

    /**
     * 把识别到的 (取件码, 快递公司) 存进 APP。
     * 已经在 APP 里的取件码（不管是待取件、已取件还是已删除）一律跳过，只添加还没有的。
     * 返回新增个数。
     */
    fun add(
        context: Context,
        found: List<Pair<String, String>>,
        source: String,
        note: String
    ): Int = synchronized(lock) {
        val store = PickupStore(context.applicationContext)
        val list = store.load()
        val known = list.map { it.code }.toSet()
        val now = System.currentTimeMillis()
        val fresh = found
            .distinctBy { it.first }
            .filter { it.first !in known }
            .map { (code, company) ->
                PickupItem(
                    id = source + "_" + UUID.randomUUID(),
                    code = code,
                    source = source,
                    note = note,
                    time = now,
                    status = PickupItem.PENDING,
                    manual = false,
                    company = company
                )
            }
        if (fresh.isNotEmpty()) store.save(list + fresh)
        fresh.size
    }
}
