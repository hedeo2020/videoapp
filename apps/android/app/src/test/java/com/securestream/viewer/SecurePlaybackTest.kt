package com.securestream.viewer
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Test
class SecurePlaybackTest { @Test fun secureFlagConstantIsStable(){ assertEquals(0x00002000,WindowManager.LayoutParams.FLAG_SECURE) } }
