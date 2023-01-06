package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@SmallTest // Unit tests
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    private val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDTO("Title2", "Description2", "location2", 20.0, 20.0)
    private val reminder3 = ReminderDTO("Title3", "Description3", "location3", 30.0, 30.0)
    private val reminder4 = ReminderDTO("Title4", "Description4", "location4", 40.0, 40.0)

    /* Initialize the fakeDataSource and the reminderListViewModel before each test */
    @Before
    fun setupRemindersListViewModel() {

        fakeDataSource = FakeDataSource()
        remindersListViewModel =
            RemindersListViewModel(
                ApplicationProvider.getApplicationContext(),
                fakeDataSource
            )
    }

    /* Clean things up after each test to start a fresh on next test */
    @After
    fun cleanup() = runTest {
        stopKoin()
        fakeDataSource.deleteAllReminders()
    }

    /**
     *
     * In [loadReminders_savingFourReminders]
     * We test loading four reminders that we inserted and matching them with the inserted ones
     * */
    @Test
    fun loadReminders_savingFourReminders() =
        mainCoroutineRule.runBlockingTest {

            // GIVEN - save four reminders to the reminderList
            fakeDataSource.saveReminder(reminder1)
            fakeDataSource.saveReminder(reminder2)
            fakeDataSource.saveReminder(reminder3)
            fakeDataSource.saveReminder(reminder4)

            // WHEN - Loading reminders form the reminderList
            remindersListViewModel.loadReminders()

            // THEN - showNoData is false, the reminderList is not empty and has 4 reminders
            assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
            assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), not(emptyList()))
            assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(4))
        }


    /**
     * In [loadReminders_savingFourRemindersFromList]
     * We test loading four reminders that we inserted using a list to the constructor of our [FakeDataSource]
     * */
    @Test
    fun loadReminders_savingFourRemindersFromList() =
        mainCoroutineRule.runBlockingTest {

            // GIVEN - List of four reminders
            val reminderList = mutableListOf(reminder1, reminder2, reminder3, reminder4)

            // WHEN - Adding them to the fakeDataSource via its constructor an load the reminders
            fakeDataSource = FakeDataSource(reminderList)
            remindersListViewModel =
                RemindersListViewModel(
                    ApplicationProvider.getApplicationContext(),
                    fakeDataSource
                )
            remindersListViewModel.loadReminders()

            // THEN - showNoData is false, the reminderList is not empty and has 4 reminders
            assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
            assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), not(emptyList()))
            assertThat(
                remindersListViewModel.remindersList.getOrAwaitValue().size,
                `is`(reminderList.size)
            )
        }

    /**
     * In [loadRemindersWhenTheyAreUnavailable_snackBarAppearToShowError]
     * We test if loading reminders from empty reminder list will return error
     * */
    @Test
    fun loadRemindersWhenTheyAreUnavailable_snackBarAppearToShowError() =
        mainCoroutineRule.runBlockingTest {

            // GIVEN - No Reminders
            fakeDataSource.deleteAllReminders()

            // WHEN - Triggering an error then load the reminders
            fakeDataSource.setReturnError(true)

            remindersListViewModel.loadReminders()

            // THEN - We get the "No reminders available" error message
            assertThat(
                remindersListViewModel.showSnackBar.getOrAwaitValue(),
                `is`("No reminders available")
            )
        }

    /**
     * In [loadReminders_loading] we test the showLoading when loading reminders
     * */
    @Test
    fun loadReminders_loading() = mainCoroutineRule.runBlockingTest {
        // Pausing dispatcher to verify initial values
        mainCoroutineRule.pauseDispatcher()

        // GIVEN - No reminders
        fakeDataSource.deleteAllReminders()

        // WHEN - Loading the reminders
        remindersListViewModel.loadReminders()

        // THEN - Loading indicator is shown
        assertThat(
            remindersListViewModel.showLoading.value, `is`(true)
        )

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // THEN - Loading indicator is hidden again
        assertThat(
            remindersListViewModel.showLoading.value, `is`(false)
        )
    }
}