package com.example.connectmeapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class LoginTest {
    @get:Rule
    var activityRule = ActivityScenarioRule(SecondScreen::class.java)

    @Before
    fun setup() {
        // Initialize Intents before testing
        Intents.init()
    }

    @Test
    fun testLoginSuccess() {
        // Enter email and password
        onView(withId(R.id.email_input)).perform(typeText("i221197@nu.edu.pk"))
        closeSoftKeyboard()

        onView(withId(R.id.password_input)).perform(typeText("123456789"))
        closeSoftKeyboard()

        // Click login button
        onView(withId(R.id.login_button)).perform(click())

        Thread.sleep(3000)

        // Check if MainActivity has loaded by verifying if BottomNavigation is displayed
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }
}
