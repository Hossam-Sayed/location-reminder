package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import com.udacity.project4.BuildConfig
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*


private const val TAG = "SelectLocationFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var poiMarker: Marker
    private lateinit var pointOfInterest: PointOfInterest

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {


        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_select_location, container, false
        )

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.saveBtn.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {

        if (this::pointOfInterest.isInitialized) {
            _viewModel.selectedPOI.value = pointOfInterest
            _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
            _viewModel.latitude.value = pointOfInterest.latLng.latitude
            _viewModel.longitude.value = pointOfInterest.latLng.longitude
            findNavController().navigateUp()
        } else {
            Toast.makeText(requireContext(), R.string.select_location, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setPoiClick(map)
        setMapClick(map)
        setMapStyle(map)
        enableMyLocationAndNavigateToUserLocation()
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            pointOfInterest = poi
            if (this::poiMarker.isInitialized) poiMarker.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )!!
            poiMarker.showInfoWindow()
        }
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            pointOfInterest = PointOfInterest(latLng, "", snippet)
            if (this::poiMarker.isInitialized) poiMarker.remove()
            poiMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )!!
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                showOkSnackBar()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }

    private fun enableMyLocationAndNavigateToUserLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED -> {

                if (this::map.isInitialized && !map.isMyLocationEnabled) {
                    map.isMyLocationEnabled = true
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val homeLatLng = LatLng(location.latitude, location.longitude)
                        val zoomLevel = 20f
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        enableMyLocationAndNavigateToUserLocation()
    }

    /**
     * Starts the permission check
     */
    private fun checkPermissions() {
        if (foregroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundLocationPermission()
        }
    }

    @TargetApi(29)
    private fun foregroundLocationPermissionApproved() = (
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))

    @TargetApi(29)
    private fun requestForegroundLocationPermission() {
        if (foregroundLocationPermissionApproved())
            return

        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when {
            // if foreground location permission is granted, enable my-location layer
            // then check device location settings
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED -> {
                map.isMyLocationEnabled = true // enable my-location layer
                checkDeviceLocationSettings() // check device location settings
            }
            // if not granted, show the snackBar
            else -> {
                showSettingsSnackBar()
            }
        }
    }

    private fun showSettingsSnackBar() {
        Snackbar.make(
            binding.mapLayout, // The binding fragment that the SnackBar will appear in
            R.string.permission_denied_explanation, // String to be shown on the SnackBar
            Snackbar.LENGTH_LONG // Make the SnackBar remains on the screen for long period
        )
            // Sets an action that will appear on the SnackBar
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    private fun showOkSnackBar() {
        Snackbar.make(
            binding.mapLayout,
            R.string.location_required_error,
            Snackbar.LENGTH_LONG
        ).setAction(android.R.string.ok) {
            checkDeviceLocationSettings()
        }.show()
    }
}
