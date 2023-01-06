package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@MediumTest // Integration tests
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersLocalRepositoryTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 10.0, 10.0)
    private val reminder2 = ReminderDTO("Title2", "Description2", "location2", 20.0, 20.0)
    private val reminder3 = ReminderDTO("Title3", "Description3", "location3", 30.0, 30.0)
    private val reminder4 = ReminderDTO("Title4", "Description4", "location4", 40.0, 40.0)

     /* Initializing the database and the local repository before each test
     We use inMemoryDatabaseBuilder to build an in-memory database as it will be killed after killing the process */
    @Before
    fun setup() {

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localRepository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    /* Close the database after every test to start a fresh one the next test */
    @After
    fun cleanUp() = database.close()

    /**
     * In [saveReminders]
     * We test [saveReminder] function of [localRepository] by saving four reminders
     * then counting the number of reminder in the database
     * */
    @Test
    fun saveReminders() = runBlocking {

        // GIVEN - Inserting four reminders
        localRepository.saveReminder(reminder1)
        localRepository.saveReminder(reminder2)
        localRepository.saveReminder(reminder3)
        localRepository.saveReminder(reminder4)

        // WHEN - Counting the size of the reminders in the database
        val count = (localRepository.getReminders() as Result.Success).data.size

        // THEN - We have four reminders in the database
        assertThat(count, `is`(4))
    }

    /**
     * In [saveReminderAndGetById]
     * We test [getReminder] function of [localRepository] by saving a reminder
     * and get it back with its ID
     * */
    @Test
    fun saveReminderAndGetById() = runBlocking {

        // GIVEN - Saving a reminder.
        localRepository.saveReminder(reminder1)

        // WHEN - Getting the reminder by id.
        val loadedReminder = localRepository.getReminder(reminder1.id) as Result.Success

        // THEN - Loaded data contains expected values.
        assertThat(loadedReminder.data, notNullValue())
        assertThat(loadedReminder.data.id, `is`(reminder1.id))
        assertThat(loadedReminder.data.title, `is`(reminder1.title))
        assertThat(loadedReminder.data.description, `is`(reminder1.description))
        assertThat(loadedReminder.data.location, `is`(reminder1.location))
        assertThat(loadedReminder.data.latitude, `is`(reminder1.latitude))
        assertThat(loadedReminder.data.longitude, `is`(reminder1.longitude))
    }

    /**
     * In [saveRemindersAndGetThem]
     * We test [getReminders] function of [localRepository] by saving four reminders
     * then getting them back
     * */
    @Test
    fun saveRemindersAndGetThem() = runBlocking {

        // GIVEN - Inserting all the four reminders
        localRepository.saveReminder(reminder1)
        localRepository.saveReminder(reminder2)
        localRepository.saveReminder(reminder3)
        localRepository.saveReminder(reminder4)

        // WHEN - Loading all the reminders
        val loadedReminders = localRepository.getReminders() as Result.Success

        // THEN - We get all the reminders back (which are four)
        assertThat(loadedReminders.data.size, `is`(4))
    }

    /**
     * In [saveRemindersAndDeleteThem]
     * We test [deleteAllReminders] function of [localRepository] by saving four reminders
     * then counting the number of reminders left after calling [deleteAllReminders]
     * */
    @Test
    fun saveRemindersAndDeleteThem() = runBlocking {

        // GIVEN - Inserting all the four reminders
        localRepository.saveReminder(reminder1)
        localRepository.saveReminder(reminder2)
        localRepository.saveReminder(reminder3)
        localRepository.saveReminder(reminder4)

        // WHEN - Delete reminder by Id
        localRepository.deleteAllReminders()

        // THEN - No reminder are in teh database
        assertThat((localRepository.getReminders() as Result.Success).data.size, `is`(0))
    }

    /**
     * In [getReminderById_returnsError]
     * We test if getting a reminder that doesn't exist will return an error message
     * */
    @Test
    fun getReminderById_returnsError() = runBlocking {

        // GIVEN - Empty database
        localRepository.deleteAllReminders()

        // WHEN - Getting a reminder that doesn't exist
        val result = localRepository.getReminder(reminder1.id) as Result.Error

        // THEN - We get the Result.Error message "Reminder not found!"
        Assert.assertThat(result.message, `is`("Reminder not found!"))
    }
}