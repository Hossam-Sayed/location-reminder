package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.not
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito


@MediumTest // Integration tests
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ReminderListFragmentTest : KoinTest {

    private val dataSource: ReminderDataSource by inject()

    private val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDTO("Title2", "Description2", "location2", 20.0, 20.0)
    private val reminder3 = ReminderDTO("Title3", "Description3", "location3", 30.0, 30.0)
    private val reminder4 = ReminderDTO("Title4", "Description4", "location4", 40.0, 40.0)

    /* Initializing the datasource using koin before each test */
    @Before
    fun initRepository() {
        stopKoin()
        // using Koin Library as a service locator
        val myModule = module {
            // Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    get(),
                    get()
                )
            }
            single {
                FakeAndroidDataSource() as ReminderDataSource
            }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(myModule))
        }
    }

    /* Clean things up after each test */
    @After
    fun cleanup() = runBlockingTest {
        dataSource.deleteAllReminders()
    }

    /**
     * In [reminderListFragmentEmpty_DisplayedInUi]
     * We test if launching the [ReminderListFragment] without entering any reminders
     * will show the Fragment with "No Data" text and there is no any reminders shown
     * */
    @Test
    fun reminderListFragmentEmpty_DisplayedInUi() = runBlockingTest {

        // GIVEN - Empty database
        dataSource.deleteAllReminders()

        // WHEN - Launch the ReminderListFragment
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - We see "No Data" string and we see no reminders
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))

        onView(ViewMatchers.withText(R.string.no_data))
            .check(matches(isDisplayed()))

        onView(ViewMatchers.withText(reminder1.title))
            .check(doesNotExist())

        onView(ViewMatchers.withText(reminder2.title))
            .check(doesNotExist())

        onView(ViewMatchers.withText(reminder3.title))
            .check(doesNotExist())

        onView(ViewMatchers.withText(reminder4.title))
            .check(doesNotExist())
    }

    /**
     * In [reminderListFragmentWithThreeReminders_DisplayedInUi]
     * We test if saving three reminders using saveReminder will make them appear in the recyclerView
     * */
    @Test
    fun reminderListFragmentWithThreeReminders_DisplayedInUi() = runBlockingTest {

        // GIVEN - Saving three reminders to the database
        dataSource.saveReminder(reminder1)
        dataSource.saveReminder(reminder2)
        dataSource.saveReminder(reminder3)
        dataSource.saveReminder(reminder4)

        // WHEN - Launch the ReminderListFragment
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - We don't see "No Data" string and we see the three reminders that we added
        onView(withId(R.id.noDataTextView))
            .check(matches(not(isDisplayed())))

        onView(ViewMatchers.withText(R.string.no_data))
            .check(matches(not(isDisplayed())))

        onView(ViewMatchers.withText(reminder1.title))
            .check(matches(isDisplayed()))

        onView(ViewMatchers.withText(reminder2.title))
            .check(matches(isDisplayed()))

        onView(ViewMatchers.withText(reminder3.title))
            .check(matches(isDisplayed()))

        onView(ViewMatchers.withText(reminder4.title))
            .check(matches(isDisplayed()))
    }

    /**
     * In [clickAddReminderFAB_navigateToSaveReminderFragment]
     * We test if clicking the addReminderFab will open the [SaveReminderFragment]
     * */
    @Test
    fun clickAddReminderFAB_navigateToSaveReminderFragment() = runBlockingTest {

        // GIVEN - The ReminderListFragment is launched on the screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - we click the addReminderFAB
        onView(withId(R.id.addReminderFAB))
            .perform(ViewActions.click())

        // THEN - Verify that we navigate to the SaveReminderFragment
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}