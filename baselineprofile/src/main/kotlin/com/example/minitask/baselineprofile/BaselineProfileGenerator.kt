package com.example.minitask.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() = baselineRule.collect(
        packageName = "com.example.minitask",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        val width = device.displayWidth
        val height = device.displayHeight
        val centerX = width / 2
        val verticalStart = (height * 0.80f).toInt()
        val verticalEnd = (height * 0.30f).toInt()
        val horizontalY = (height * 0.36f).toInt()
        val horizontalStart = (width * 0.84f).toInt()
        val horizontalEnd = (width * 0.16f).toInt()

        repeat(4) {
            device.swipe(centerX, verticalStart, centerX, verticalEnd, 24)
            device.waitForIdle()
            device.swipe(centerX, verticalEnd, centerX, verticalStart, 24)
            device.waitForIdle()
            device.swipe(horizontalStart, horizontalY, horizontalEnd, horizontalY, 24)
            device.waitForIdle()
            device.swipe(horizontalEnd, horizontalY, horizontalStart, horizontalY, 24)
            device.waitForIdle()
        }
    }
}
