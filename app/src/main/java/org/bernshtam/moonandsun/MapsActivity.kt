package org.bernshtam.moonandsun

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import net.time4j.PlainDate
import net.time4j.android.ApplicationStarter
import java.time.LocalDate


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRequestPermissionsResultCallback {

    private lateinit var map: MapContainer
    private lateinit var explanationContainer: ExplanationContainer


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
        for(p in allProviders){
            val lastKnownLocation = locationManager.getLastKnownLocation(p)
            if (lastKnownLocation!=null){
                val p0 = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

                map.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(p0,17.0f))
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



}

