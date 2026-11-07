package com.example.minicraft

class InputState {
    @Volatile var moveForward = false
    @Volatile var moveBack = false
    @Volatile var moveLeft = false
    @Volatile var moveRight = false
    @Volatile var jump = false
    @Volatile var breakRequested = false
    @Volatile var placeRequested = false
}
