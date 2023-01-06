package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
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
class SaveReminderViewModelTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    private val reminder1 = ReminderDataItem("Title1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDataItem(null, "Description2", "location2", 20.0, 20.0)
    private val reminder3 = ReminderDataItem("Title3", "Description3", null, 30.0, 30.0)

    /* Initialize the fakeDataSource and the saveReminderViewModel before each test */
    @Before
    fun setupRemindersListViewModel() = runBlocking {

        fakeDataSource = FakeDataSource()
        saveReminderViewModel =
            SaveReminderViewModel(
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
     * In [onClear_assignDataThenClear_dataIsNull]
     * We test [onClear] function by clearing data after we entered it
     * */
    @Test
    fun onClear_assignDataThenClear_dataIsNull() {

        // GIVEN - Adding data
        saveReminderViewModel.reminderTitle.value = reminder1.title
        saveReminderViewModel.reminderDescription.value = reminder1.description
        saveReminderViewModel.reminderSelectedLocationStr.value = reminder1.location
        saveReminderViewModel.latitude.value = reminder1.latitude
        saveReminderViewModel.longitude.value = reminder1.longitude

        // WHEN - Calling onClear to clear all data
        saveReminderViewModel.onClear()

        // THEN - All data is cleared
        assertThat(saveReminderViewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(
            saveReminderViewModel.reminderSelectedLocationStr.getOrAwaitValue(),
            `is`(nullValue())
        )
        assertThat(saveReminderViewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(saveReminderViewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }

    /**
     * In [validateEnteredData_nullReminderData_returnFalse]
     * We test [validateEnteredData] function by seeing if a reminder contains null title or location
     * it will return error message on snackBar
     * */
    @Test
    fun validateEnteredData_nullReminderData_returnFalse() {

        // GIVEN - One valid reminder (reminder1) and two invalid reminders (reminder2, 3)

        // WHEN - Calling validateEnteredData on each reminder
        // THEN - The valid reminder (reminder1) will return true and the snackBar will not be shown
        // THEN - The other invalid two will return false and the snackBar will be shown to display an error message
        assertThat(
            saveReminderViewModel.validateEnteredData(reminder1),
            `is`(true)
        )

        assertThat(
            saveReminderViewModel.validateEnteredData(reminder2),
            `is`(false)
        )
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )

        assertThat(
            saveReminderViewModel.validateEnteredData(reminder3),
            `is`(false)
        )
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    /**
     * In [saveReminder_gettingReminderBack_returnSameReminder]
     * We test [getReminder] function by saving a reminder using saveReminder
     * and getting it back using loadReminder
     * */
    @Test
    fun saveReminder_gettingReminderBack_returnSameReminder() = runBlocking {

        // GIVEN - Saving reminder1 to the reminderList
        saveReminderViewModel.saveReminder(reminder1)

        // WHEN - Getting this reminder back
        val returnedReminder = fakeDataSource.getReminder(reminder1.id) as Result.Success

        // THEN - We get back the same reminder that we saved (reminder1)
        assertThat(returnedReminder, `is`(not(nullValue())))
        assertThat(returnedReminder.data.id, `is`(reminder1.id))
        assertThat(returnedReminder.data.title, `is`(reminder1.title))
        assertThat(returnedReminder.data.description, `is`(reminder1.description))
        assertThat(returnedReminder.data.location, `is`(reminder1.location))
        assertThat(returnedReminder.data.latitude, `is`(reminder1.latitude))
        assertThat(returnedReminder.data.longitude, `is`(reminder1.longitude))
    }

    /**
     * In [saveReminder_showLoading]
     * We test the value of [showLoading] by seeing if saving a reminder using saveReminder will show loading
     * */
    @Test
    fun saveReminder_showLoading() {

        // Pausing dispatcher to verify initial values
        mainCoroutineRule.pauseDispatcher()

        // GIVEN - One reminder
        val reminder = reminder1

        // WHEN - Saving that reminder
        saveReminderViewModel.saveReminder(reminder)

        // THEN - Loading indicator is shown
        assertThat(
            saveReminderViewModel.showLoading.value, `is`(true)
        )

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // THEN - Loading indicator is hidden again
        assertThat(
            saveReminderViewModel.showLoading.value, `is`(false)
        )
    }

    /**
     * In [saveReminder_showToast]
     * We test [showToast] value by seeing if saving a reminder using saveReminder will show a toast
     * with message "Reminder Saved !"
     * */
    @Test
    fun saveReminder_showToast() = mainCoroutineRule.runBlockingTest {

        // Pausing dispatcher to verify initial values
        mainCoroutineRule.pauseDispatcher()

        // GIVEN - One reminder
        val reminder = reminder1

        // WHEN - Saving a reminder
        saveReminderViewModel.saveReminder(reminder)

        // THEN - Toast will not be shown
        assertThat(
            saveReminderViewModel.showToast.value, `is`(nullValue())
        )

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // THEN - Toast will be shown with message "Reminder Saved !"
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !")
        )
    }

    /**
     * In [getRemindersWhenTheyAreUnavailable_callErrorToDisplay]
     * We test if we call loadReminders without having any reminders will cause an error
     * with message "No reminders available"
     * */
    @Test
    fun getRemindersWhenTheyAreUnavailable_callErrorToDisplay() =
        mainCoroutineRule.runBlockingTest {
            //GIVEN - Making the datasource return errors.
            fakeDataSource.setReturnError(true)

            // WHEN - Getting the reminders
            fakeDataSource.getReminders()

            //THEN - We get the "No reminders available" error message
            assertThat(fakeDataSource.getReminders(), `is`(Result.Error("No reminders available")))
        }
}