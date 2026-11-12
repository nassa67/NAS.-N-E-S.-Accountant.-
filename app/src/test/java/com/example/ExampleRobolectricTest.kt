package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NAS Finance", appName)
  }

  @Test
  fun `mainActivity launches and renders auth screen without crashing`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_screen").assertExists()
    composeTestRule.onNodeWithText("NAS Finance").assertExists()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
  }
}

