package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.android.synthetic.main.activity_reminder_description.*
import kotlinx.android.synthetic.main.fragment_save_reminder.*
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    val viewModel: ReminderDescriptionViewModel by inject()

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    private lateinit var binding: ActivityReminderDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        val reminder = intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem
        if (reminder.description == null) {
            description_headline_text.visibility = View.GONE
            reminder_description_text.visibility = View.GONE
        }
        binding.reminderDataItem = reminder

        viewModel.deleteReminderById(reminder)
    }
}
