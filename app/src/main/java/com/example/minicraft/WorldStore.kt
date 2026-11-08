package com.example.minicraft

import android.content.Context
import java.io.File

data class WorldInfo(val name: String, val file: File)

object WorldStore {
    fun worldsDir(ctx: Context): File {
        val dir = File(ctx.filesDir, "worlds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun list(ctx: Context): List<WorldInfo> {
        val dir = worldsDir(ctx)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".dat") }
            ?.map { WorldInfo(it.name.removeSuffix(".dat"), it) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun create(ctx: Context, rawName: String): WorldInfo? {
        val name = rawName.trim().ifEmpty { return null }
        val safe = name.replace(Regex("[^A-Za-z0-9_а-яА-ЯёЁ\\- ]"), "_")
        val file = File(worldsDir(ctx), "$safe.dat")
        if (file.exists()) return null
        val w = World(file)
        w.save(file)
        return WorldInfo(safe, file)
    }

    fun delete(info: WorldInfo): Boolean = info.file.delete()
}
