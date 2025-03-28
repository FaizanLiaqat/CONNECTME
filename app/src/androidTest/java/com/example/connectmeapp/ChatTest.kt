package com.example.connectmeapp

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatTest {

    @get:Rule
    var activityRule = ActivityScenarioRule(ChatBoxActivity::class.java)

    @Test
    fun testMessageSending() {
        val testMessage = "Hello, testing!"
        onView(withId(R.id.message_input)).perform(typeText(testMessage))
        closeSoftKeyboard()
        onView(withId(R.id.send_icon)).perform(click())

        // Wait for the message to appear
        Thread.sleep(2000)

        // Get last index dynamically
        val lastIndex = getRecyclerViewItemCount(R.id.chat_recycler_view) - 1

        // Scroll to the last item
        onView(withId(R.id.chat_recycler_view))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(lastIndex))

        // Verify the last message is displayed
        onView(allOf(withId(R.id.text_message), withText(testMessage)))
            .check(matches(isDisplayed()))
    }

    // Function to get RecyclerView item count
    private fun getRecyclerViewItemCount(recyclerViewId: Int): Int {
        var itemCount = 0
        activityRule.scenario.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(recyclerViewId)
            itemCount = recyclerView.adapter?.itemCount ?: 0
        }
        return itemCount
    }
}
