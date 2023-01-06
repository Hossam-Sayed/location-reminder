package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val TAG = "SaveReminderFragment"
private const val GEOFENCE_RADIUS_IN_METERS = 100f

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var reminder: ReminderDataItem

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private lateinit var permissionsResultLauncher: ActivityResultLauncher<Array<String>>
    private val requestMultiplePermissions = ActivityResultContracts.RequestMultiplePermissions()
    private lateinit var locationResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val startIntentSender = ActivityResultContracts.StartIntentSenderForResult()

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(
            requireContext()
        )
    }

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = "ACTION_GEOFENCE_EVENT"
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        permissionsResultLauncher =
            registerForActivityResult(requestMultiplePermissions) {
                checkAllPermissionsAndStartGeofencing()
            }

        locationResultLauncher = registerForActivityResult(startIntentSender) { result ->
            if (result.resultCode == RESULT_OK)
                addGeofence()
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminder = ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateEnteredData(reminder)) {
                checkAllPermissionsAndStartGeofencing()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofencing() {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    Toast.makeText(
                        context,
                        R.string.permission_explanation,
                        Toast.LENGTH_SHORT
                    ).show()
                    locationResultLauncher.launch(
                        IntentSenderRequest.Builder(
                            exception.resolution
                        )
                            .build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    /**
     * [checkAllPermissionsAndStartGeofencing] function checks if all the permissions are
     * granted (foreground and background) if one is not, then it request it
     * if both of them were not granted then it request both
     * */
    private fun checkAllPermissionsAndStartGeofencing() {

        // if both are approved
        if (foregroundLocationPermissionApproved() && backgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofencing()
        }
        // if foreground is only approved
        else if (foregroundLocationPermissionApproved() && !backgroundLocationPermissionApproved()) {
            requestBackgroundLocationPermission()
        }
        // if background was only approved
        else if (!foregroundLocationPermissionApproved() && backgroundLocationPermissionApproved()) {
            requestForegroundLocationPermission()
        }
        // if neither was approved
        else {
            requestForegroundLocationPermission()
            requestBackgroundLocationPermission()
        }
    }

    /**
     * [requestForegroundLocationPermission] function checks:
     * 1. If the foreground permission is
     * already approved then it returns (nothing to request).
     *
     * 2. if the permission is requested before and denied then we show a snackbar.
     *
     * 3. Else we request the permission from the user (for the first time).
     * */
    private fun requestForegroundLocationPermission() {
        if (foregroundLocationPermissionApproved()) {
            return
        } else {
            permissionsResultLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * [foregroundLocationPermissionApproved] function returns true if the foreground location
     * permission is approved
     * */
    private fun foregroundLocationPermissionApproved() =
        ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED


    /**
     * [requestBackgroundLocationPermission] function checks first if the background location
     * permission is approved, if not, it will require that permission from the user if he is
     * running android Q or later
     * */
    @TargetApi(29)
    private fun requestBackgroundLocationPermission() {
        if (backgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofencing()
            return
        }
        if (runningQOrLater) {
            Toast.makeText(context, R.string.permission_explanation, Toast.LENGTH_SHORT).show()
            permissionsResultLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        } else {

            // The device is running android lower than Q,
            // then it doesn't support background location permission,
            // so, return from the function as there is nothing to request
            return
        }
    }

    /**
     * [backgroundLocationPermissionApproved] function returns true if the background location
     * permission is approved on an android device running android Q or later
     * */
    @TargetApi(29)
    private fun backgroundLocationPermissionApproved() =
        if (runningQOrLater) {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {

            // The device is running android lower than Q,
            // then it doesn't support the background location permission,
            // so, return true
            true
        }

    private fun addGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(NEVER_EXPIRE) // make the geofence never expires
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.e("Add Geofence", geofence.requestId)
                _viewModel.validateAndSaveReminder(reminder)
            }
            addOnFailureListener {
                if ((it.message != null)) {
                    Log.w(TAG, it.message!!)
                }
            }
        }
    }
}