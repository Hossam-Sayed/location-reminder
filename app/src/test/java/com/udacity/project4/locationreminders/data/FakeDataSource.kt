package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.util.LinkedHashMap

class FakeDataSource(private val reminders: MutableList<ReminderDTO>? = mutableListOf()) :
    ReminderDataSource {

    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {

        if (shouldReturnError) {
            return Result.Error("No reminders available")
        }

        reminders.let { return Result.Success(ArrayList(it!!)) }

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {

        if (shouldReturnError) {
            return Result.Error("Reminder not found")
        }

        reminders?.find {
            it.id == id
        }?.let { return Result.Success(it) }

        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

    override suspend fun deleteReminder(id: String) {
        reminders?.find {
            it.id == id
        }?.let { reminder ->
            reminders.remove(reminder)
        }
    }
}