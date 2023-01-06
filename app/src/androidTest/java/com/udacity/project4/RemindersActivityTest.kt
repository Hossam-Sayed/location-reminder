package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.hamcrest.core.IsNot.not
import org.koin.test.KoinTest


@LargeTest // End-to-end tests
@RunWith(AndroidJUnit4::class)
class RemindersActivityTest : KoinTest {

    private lateinit var repository: ReminderDataSource

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val activityRule = ActivityTestRule(RemindersActivity::class.java)

    /* Getting the context */
    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }

    /* Initializing repository using koin for testing before each test */
    @Before
    fun init() {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }
        // Declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get the real repository
        repository = get()

        // Clear reminders to start without any reminders
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /* Register the Idling Resource before each test */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /* Unregister the Idling Resource after each test
     so it can be collected by garbage collector and does not cause any memory leaks */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /**
     * In [showReminderSavedToast]
     * We test if inserting required data in the [SaveReminderFragment]
     * will show a toast after hitting the save FAB
     * */
    @ExperimentalCoroutinesApi
    @Test
    fun showReminderSavedToast() = runBlocking {

        // GIVEN - Launching the Reminders Activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // WHEN - Entering data in the fields and open the map to select a random location
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(
            typeText("Description"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.map)).perform(click())
        onView(withId(R.id.save_btn)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())


        // THEN - A toast will appear with the message "Reminder Saved !" on it
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(not(`is`(getActivity(activityScenario)?.window?.decorView)))
        )
            .check(matches(isDisplayed()))

        activityScenario.close()
    }

    /**
     * In [snackBar_enterTitle]
     * We test if clicking the addReminderFAB with empty title will show a snackBar
     * with message "Please enter title" on it
     * */
    @Test
    fun snackBar_enterTitle() {

        // GIVEN - Launching the Reminders Activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // WHEN - Clicking on add reminder to save a reminder with empty title
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - A snackBar is displayed with the message "Please enter title"
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        activityScenario.close()
    }

    /**
     * In [snackBar_enterLocation]
     * We test if clicking the addReminderFAB with a title but without a location will show a snackBar
     * with message "Please enter location" on it
     * */
    @Test
    fun snackBar_enterLocation() {

        // GIVEN - Launching the Reminders Activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // WHEN - Clicking on add reminder to save a reminder with empty location
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(replaceText("Title"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())

        // THEN - A snackBar is displayed with the message "Please enter location"
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))

        activityScenario.close()
    }
}
