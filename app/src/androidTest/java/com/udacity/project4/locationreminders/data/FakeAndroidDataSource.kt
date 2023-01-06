package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeAndroidDataSource(private val reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        reminders.let { return Result.Success(it) }

        return Result.Error("No reminders available")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        reminders.find {
            it.id == id
        }?.let { return Result.Success(it) }

        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    override suspend fun deleteReminder(id: String) {
        reminders.find {
            it.id == id
        }?.let { reminder ->
            reminders.remove(reminder)
        }
    }
}