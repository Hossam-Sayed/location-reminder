package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@SmallTest // Unit tests
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDTO("Title2", "Description2", "location2", 20.0, 20.0)
    private val reminder3 = ReminderDTO("Title3", "Description3", "location3", 30.0, 30.0)
    private val reminder4 = ReminderDTO("Title4", "Description4", "location4", 40.0, 40.0)

    /* Initializing the database before each test */
    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    /* Close the database after every test to start a fresh one the next test */
    @After
    fun closeDb() = database.close()

    /**
     * In [saveReminders]
     * We test if the number of reminders in the database is 4 after adding four reminders
     * using saveReminder method
     * */
    @Test
    fun saveReminders() = runBlockingTest() {

        // GIVEN - Inserting four reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)
        database.reminderDao().saveReminder(reminder4)

        // WHEN - Counting the size of the reminders in the database
        val count = database.reminderDao().getReminders().size

        // THEN - We have four reminders in the database
        assertThat(count, `is`(4))
    }

    /**
     * In [saveReminderAndGetById]
     * We test [getReminderById] function by saving a reminder into teh database and getting it back
     * then comparing its values with the added one
     * */
    @Test
    fun saveReminderAndGetById() = runBlockingTest {

        // GIVEN - Insert a reminder.
        database.reminderDao().saveReminder(reminder1)

        // WHEN - Get the reminder by id from the database.
        val loadedReminder = database.reminderDao().getReminderById(reminder1.id)

        // THEN - Loaded data contains expected values.
        assertThat(loadedReminder as ReminderDTO, CoreMatchers.notNullValue())
        assertThat(loadedReminder.id, `is`(reminder1.id))
        assertThat(loadedReminder.title, `is`(reminder1.title))
        assertThat(loadedReminder.description, `is`(reminder1.description))
        assertThat(loadedReminder.location, `is`(reminder1.location))
        assertThat(loadedReminder.latitude, `is`(reminder1.latitude))
        assertThat(loadedReminder.longitude, `is`(reminder1.longitude))
    }

    /**
     * In [saveRemindersAndGetThem]
     * We test [getReminders] function by saving four reminders to the database
     * and counting the number of reminders in the database after getting them
     * */
    @Test
    fun saveRemindersAndGetThem() = runBlockingTest {

        // GIVEN - Inserting all the four reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)
        database.reminderDao().saveReminder(reminder4)

        // WHEN - Loading all the reminders
        val loadedReminders = database.reminderDao().getReminders()

        // THEN - We get all the reminders back (which are four)
        assertThat(loadedReminders.size, `is`(4))
    }

    /**
     * In [saveRemindersAndDeleteThem]
     * We test [deleteAllReminders] function by saving four reminders to the database
     * then counting the number of reminders left after calling [deleteAllReminders]
     * */
    @Test
    fun saveRemindersAndDeleteThem() = runBlockingTest {

        // GIVEN - Inserting all the four reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)
        database.reminderDao().saveReminder(reminder4)

        // WHEN - Delete reminder by Id
        database.reminderDao().deleteAllReminders()

        // THEN - No reminder are in teh database
        assertThat(database.reminderDao().getReminders().size, `is`(0))
    }

    /**
     * In [insertRemindersAndDeleteReminderById]
     * We test [deleteReminderById] function by saving a reminder then deleting it with its id
     * then confirming that the number of reminders left in the database is three
     * */
    @Test
    fun insertRemindersAndDeleteReminderById() = runBlockingTest{
        // GIVEN - Saving all four reminders
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)
        database.reminderDao().saveReminder(reminder4)

        // WHEN - Deleting reminder3
        database.reminderDao().deleteReminderById(reminder3.id)

        // THEN - Reminder4 will take the position od reminder3 in the database (third position)
        val reminders = database.reminderDao().getReminders()
        assertThat(reminders[2].id, `is` (reminder4.id))

        // THEN - The number of reminders in the database will be 3 (reminder3 is removed)
        assertThat(reminders.size, `is`(3))

    }
}