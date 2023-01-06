package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AuthenticationActivity"
    }

    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val binding = DataBindingUtil.setContentView<ActivityAuthenticationBinding>(
            this,
            R.layout.activity_authentication
        )

        binding.loginBtn.setOnClickListener { launchSignInFlow() }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Log.d(TAG, "Login Successful")
                }
            }

        viewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                }
                else -> Log.d(TAG, "Failed to login")
            }
        }
    }

    private fun launchSignInFlow() {

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val loginCustomLayout = AuthMethodPickerLayout.Builder(R.layout.login_custom_layout)
            .setEmailButtonId(R.id.email_btn)
            .setGoogleButtonId(R.id.google_btn)
            .build()

        activityResultLauncher.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(loginCustomLayout)
                .build())
    }
}
