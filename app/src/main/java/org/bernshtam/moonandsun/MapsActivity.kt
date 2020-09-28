package org.bernshtam.moonandsun

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.kotlindemos.PermissionUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_maps.*
import net.time4j.android.ApplicationStarter
import java.time.LocalDate
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRequestPermissionsResultCallback,
    DatePickerDialog.OnDateSetListener {

    private lateinit var map: MapContainer
    private lateinit var explanationContainer: ExplanationContainer
    private var date = LocalDate.now()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApplicationStarter.initialize(this, true)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        explanationContainer = ExplanationContainer(
            explanation,
            getString(R.string.Sunrise),
            getString(R.string.Sunset),
            getString(R.string.Moonrise),
            getString(R.string.Moonset),
            getString(R.string.MoonIllumination)
        )


        if (actionBar != null) {
            actionBar?.title =  DATE.format(date)
       }

         if (supportActionBar != null) {
             supportActionBar?.title =  DATE.format(date)
        }
        val viewId = resources.getIdentifier("action_bar_title", "id", "android")

        explanation.setOnClickListener {
            DatePickerDialog(
                this@MapsActivity, this,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }


    override fun onMapReady(googleMap: GoogleMap?) {

        googleMap?.apply {
            map = MapContainer(googleMap, explanationContainer, LocalDate.now())
            updateMyLocation()
        }
    }

    private fun updateMyLocation() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            canGetLocation()
        } else {

            PermissionUtils.requestPermission(
                this, 1,
                Manifest.permission.ACCESS_FINE_LOCATION, false
            )
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
        if (requestCode != 1) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            canGetLocation()
            //  mMyLocationCheckbox.setChecked(true)
        }/* else {
            //  mShowPermissionDeniedDialog = true
        }*/
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        date = LocalDate.of(year,month+1,dayOfMonth)
        if (actionBar != null) {
            actionBar?.title =  DATE.format(date)
        }

        if (supportActionBar != null) {
            supportActionBar?.title =  DATE.format(date)
        }
        map.removeMarkers()
        map = MapContainer(map.mMap, explanationContainer, date)
        updateMyLocation()
    }


}

