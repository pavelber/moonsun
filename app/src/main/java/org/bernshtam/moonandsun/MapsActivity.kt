package org.bernshtam.moonandsun

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat.checkSelfPermission
import com.example.kotlindemos.PermissionUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import net.time4j.android.ApplicationStarter
import java.time.LocalDate
import java.util.*
import org.bernshtam.moonandsun.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRequestPermissionsResultCallback,
    DatePickerDialog.OnDateSetListener {

    private lateinit var map: MapContainer
    private lateinit var explanationContainer: ExplanationContainer
    private var date = LocalDate.now()
    private lateinit var binding: ActivityMapsBinding
    private val cal = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApplicationStarter.initialize(this, true)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        explanationContainer = ExplanationContainer(
            binding.explanation,
            getString(R.string.Sunrise),
            getString(R.string.Sunset),
            getString(R.string.Moonrise),
            getString(R.string.Moonset),
            getString(R.string.MoonIllumination)
        )


        putDateText()
        val apiKey = getString(R.string.maps_api_key)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        binding.searchButton.setOnClickListener {
            val fields: List<Place.Field> =
                listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION)

            // Start the autocomplete intent.
            val intent: Intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                fields
            ).build(this)
            startActivityForResult(intent, 1)
        }

        binding.explanation.setOnClickListener {
            DatePickerDialog(
                this@MapsActivity, this,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                place.location?.let { p0 ->
                    map.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p0, 17.0f))
                    map.onMapClick(p0)
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // error
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMapReady(googleMap: GoogleMap) {

            map = MapContainer(googleMap, explanationContainer, LocalDate.now())
            updateMyLocation()
    }

    private fun updateMyLocation() {
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            canGetLocation()
        } else {
            PermissionUtils.requestPermission(this, 1, ACCESS_FINE_LOCATION, false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun canGetLocation() {
        map.mMap.isMyLocationEnabled = true

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val allProviders = locationManager.allProviders
        for (p in allProviders) {
            val lastKnownLocation = locationManager.getLastKnownLocation(p)
            if (lastKnownLocation != null) {
                val p0 = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                map.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p0, 17.0f))
                map.onMapClick(p0)
                break
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 1) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions, grantResults,
                ACCESS_FINE_LOCATION
            )
        ) {
            canGetLocation()
            //  mMyLocationCheckbox.setChecked(true)
        }/* else {
            //  mShowPermissionDeniedDialog = true
        }*/
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        date = LocalDate.of(year, month + 1, dayOfMonth)
        putDateText()
        map.removeMarkers()
        map = MapContainer(map.mMap, explanationContainer, date)
        updateMyLocation()
    }

    private fun putDateText() {
        if (actionBar != null) {
            actionBar?.title = DATE.format(date)
        }

        if (supportActionBar != null) {
            supportActionBar?.title = DATE.format(date)
        }
    }


}
