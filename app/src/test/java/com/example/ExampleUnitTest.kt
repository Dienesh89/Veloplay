package com.example

import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("0:24", FormatUtils.formatDuration(24_000L))
        assertEquals("2:05", FormatUtils.formatDuration(125_000L))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000L))
    }

    @Test
    fun formatSpeed_supportsUpTo8x() {
        assertEquals("0.25x", FormatUtils.formatSpeed(0.25f))
        assertEquals("1.0x", FormatUtils.formatSpeed(1.0f))
        assertEquals("4.5x", FormatUtils.formatSpeed(4.5f))
        assertEquals("8.0x", FormatUtils.formatSpeed(8.0f))
    }

    @Test
    fun formatPitch_formatsTwoDecimals() {
        assertEquals("0.50x", FormatUtils.formatPitch(0.5f))
        assertEquals("1.00x", FormatUtils.formatPitch(1.0f))
        assertEquals("2.00x", FormatUtils.formatPitch(2.0f))
    }

    @Test
    fun formatResolutionLabel_mapsStandardHeights() {
        assertEquals("4K", FormatUtils.formatResolutionLabel(3840, 2160))
        assertEquals("1080p", FormatUtils.formatResolutionLabel(1920, 1080))
        assertEquals("720p", FormatUtils.formatResolutionLabel(720, 1280))
    }
}
