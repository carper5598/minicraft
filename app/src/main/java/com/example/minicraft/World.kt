package com.example.minicraft

import kotlin.math.cos
import kotlin.math.sin

class World(
    val sx: Int = 64,
    val sy: Int = 32,
    val sz: Int = 64
) {
    private val blocks = ByteArray(sx * sy * sz)

    init {
        generate()
    }

    private fun idx(x: Int, y: Int, z: Int): Int = (y * sz + z) * sx + x

    fun getBlock(x: Int, y: Int, z: Int): Byte {
        if (x < 0 || x >= sx || y < 0 || y >= sy || z < 0 || z >= sz) return 0
        return blocks[idx(x, y, z)]
    }

    fun isSolid(x: Int, y: Int, z: Int): Boolean {
        if (y < 0) return true
        if (y >= sy) return false
        return getBlock(x, y, z) != 0.toByte()
    }

    fun setBlock(x: Int, y: Int, z: Int, type: Byte) {
        if (x in 0 until sx && y in 0 until sy && z in 0 until sz) {
            blocks[idx(x, y, z)] = type
        }
    }

    private fun generate() {
        for (x in 0 until sx) {
            for (z in 0 until sz) {
                val h = 8 + (sin(x * 0.25) * 2.0 + cos(z * 0.3) * 2.0).toInt()
                for (y in 0..h) {
                    val type: Byte = when {
                        y == h -> 1
                        y > h - 3 -> 2
                        else -> 3
                    }
                    setBlock(x, y, z, type)
                }
            }
        }
    }

    fun buildMesh(): FloatArray {
        val data = ArrayList<Float>(400_000)
        val order = intArrayOf(0, 1, 2, 0, 2, 3)

        val top = floatArrayOf(0f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f)
        val bottom = floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f, 0f)
        val posX = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 1f)
        val negX = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 0f, 0f)
        val posZ = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 0f, 1f)
        val negZ = floatArrayOf(1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f)

        fun addFace(
            x: Int, y: Int, z: Int,
            offsets: FloatArray,
            r: Float, g: Float, b: Float,
            shade: Float
        ) {
            for (i in order) {
                val o = i * 3
                data.add(x + offsets[o])
                data.add(y + offsets[o + 1])
                data.add(z + offsets[o + 2])
                data.add(r * shade)
                data.add(g * shade)
                data.add(b * shade)
            }
        }

        for (y in 0 until sy) {
            for (z in 0 until sz) {
                for (x in 0 until sx) {
                    val block = getBlock(x, y, z)
                    if (block == 0.toByte()) continue

                    val r: Float
                    val g: Float
                    val b: Float
                    when (block.toInt()) {
                        1 -> { r = 0.30f; g = 0.75f; b = 0.30f }
                        2 -> { r = 0.55f; g = 0.35f; b = 0.20f }
                        else -> { r = 0.55f; g = 0.55f; b = 0.55f }
                    }

                    if (getBlock(x, y + 1, z) == 0.toByte()) addFace(x, y, z, top, r, g, b, 1.0f)
                    if (getBlock(x, y - 1, z) == 0.toByte()) addFace(x, y, z, bottom, r, g, b, 0.5f)
                    if (getBlock(x + 1, y, z) == 0.toByte()) addFace(x, y, z, posX, r, g, b, 0.8f)
                    if (getBlock(x - 1, y, z) == 0.toByte()) addFace(x, y, z, negX, r, g, b, 0.8f)
                    if (getBlock(x, y, z + 1) == 0.toByte()) addFace(x, y, z, posZ, r, g, b, 0.7f)
                    if (getBlock(x, y, z - 1) == 0.toByte()) addFace(x, y, z, negZ, r, g, b, 0.7f)
                }
            }
        }

        val result = FloatArray(data.size)
        for (i in data.indices) result[i] = data[i]
        return result
    }
}
