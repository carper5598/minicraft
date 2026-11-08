package com.example.minicraft

import java.util.concurrent.atomic.AtomicIntegerArray

class InputState {
    @Volatile var moveForward = false
    @Volatile var moveBack = false
    @Volatile var moveLeft = false
    @Volatile var moveRight = false
    @Volatile var jump = false
    @Volatile var breakRequested = false
    @Volatile var placeRequested = false

    // 1 = трава, 2 = земля, 3 = камень. Индекс 0 не используется.
    val inventory = AtomicIntegerArray(4)

    // Текущий выбранный блок в хотбаре
    @Volatile var selectedSlot: Int = 1
}
