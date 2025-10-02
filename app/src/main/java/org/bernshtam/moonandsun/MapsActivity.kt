package org.bernshtam.moonandsun

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
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
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import net.time4j.android.ApplicationStarter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import org.bernshtam.moonandsun.databinding.ActivityMapsBinding
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.places.api.model.AutocompleteSessionToken

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRequestPermissionsResultCallback,
    DatePickerDialog.OnDateSetListener {

    private lateinit var map: MapContainer
    private lateinit var explanationContainer: ExplanationContainer
    private var date = LocalDate.now()
    private lateinit var binding: ActivityMapsBinding
    private val cal = Calendar.getInstance()
    private var placeSuggestions = mutableListOf<AutocompleteData>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var placesClient: PlacesClient
    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val SEARCH_DEBOUNCE_MS = 300L

    // Store the selected location to preserve it when date changes
    private var selectedLocation: LatLng? = null

    companion object {
        private val DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    }

    data class AutocompleteData(val prediction: AutocompletePrediction, val displayName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApplicationStarter.initialize(this, true)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check network connectivity first
        if (!isNetworkAvailable()) {
            android.util.Log.e("MapsActivity", "No network connectivity available")
            Toast.makeText(this, "No internet connection. Google services may not work.", Toast.LENGTH_LONG).show()
        } else {
            android.util.Log.d("MapsActivity", "Network connectivity is available")
        }

        // Initialize Places SDK with detailed logging
        val apiKey = getString(R.string.maps_api_key)
        android.util.Log.d("MapsActivity", "API Key length: ${apiKey.length}")
        android.util.Log.d("MapsActivity", "API Key prefix: ${apiKey.take(10)}...")

        if (!Places.isInitialized()) {
            android.util.Log.d("MapsActivity", "Initializing Places SDK...")
            try {
                // Provide locale explicitly to avoid legacy fallback
                Places.initialize(applicationContext, apiKey, Locale.getDefault())
                android.util.Log.d("MapsActivity", "Places SDK initialized successfully (v4)")
            } catch (e: Exception) {
                android.util.Log.e("MapsActivity", "Failed to initialize Places SDK", e)
                Toast.makeText(this, "Places API initialization failed. Check API key and billing.", Toast.LENGTH_LONG).show()
                return // Exit early if Places can't be initialized
            }
        } else {
            android.util.Log.d("MapsActivity", "Places SDK already initialized")
        }

        try {
            placesClient = Places.createClient(this)
            android.util.Log.d("MapsActivity", "Places client created successfully")
        } catch (e: Exception) {
            android.util.Log.e("MapsActivity", "Failed to create Places client", e)
            Toast.makeText(this, "Places client creation failed.", Toast.LENGTH_LONG).show()
        }

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

        setupSearchFunctionality()
        putDateText()

        // Existing tap-on-explanation to change date
        binding.explanation.setOnClickListener {
            showDatePicker()
        }
        // New calendar button restored
        binding.calendarButton?.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this@MapsActivity, this,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupSearchFunctionality() {
        // Setup adapter for autocomplete suggestions
        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        binding.searchEditText.setAdapter(adapter)

        // Toggle search box visibility when search button is clicked
        binding.searchButton.setOnClickListener {
            if (binding.searchEditText.visibility == View.GONE) {
                binding.searchEditText.alpha = 0f
                binding.searchEditText.visibility = View.VISIBLE
                binding.searchEditText.animate().alpha(1f).setDuration(180).start()
                binding.searchEditText.requestFocus()
                // Show keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                // Fade out then hide
                binding.searchEditText.animate().alpha(0f).setDuration(120).withEndAction {
                    binding.searchEditText.visibility = View.GONE
                    binding.searchEditText.text.clear()
                }.start()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
            }
        }

        // Handle text changes for autocomplete
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                if (query.length >= 2) {
                    searchRunnable = Runnable { searchPlaces(query) }
                    searchHandler.postDelayed(searchRunnable!!, SEARCH_DEBOUNCE_MS)
                } else {
                    placeSuggestions.clear()
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                }
            }
        })

        // Handle item selection and enter key
        binding.searchEditText.setOnItemClickListener { _, _, position, _ ->
            selectPlace(position)
        }

        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (placeSuggestions.isNotEmpty()) {
                    selectPlace(0) // Select first suggestion
                }
                true
            } else {
                false
            }
        }
    }

    private fun newSession() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }

    private fun searchPlaces(query: String) {
        android.util.Log.d("MapsActivity", "Starting search for query: '$query' with session token: ${sessionToken.hashCode()}")

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(sessionToken)
            .build()

        android.util.Log.d("MapsActivity", "Sending autocomplete request to Places API...")

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                android.util.Log.d("MapsActivity", "Places search SUCCESS - found ${response.autocompletePredictions.size} predictions")
                placeSuggestions.clear()
                val suggestions = mutableListOf<String>()

                for (prediction in response.autocompletePredictions) {
                    val displayName = prediction.getFullText(null).toString()
                    placeSuggestions.add(AutocompleteData(prediction, displayName))
                    suggestions.add(displayName)
                    android.util.Log.d("MapsActivity", "Prediction: $displayName")
                }

                runOnUiThread {
                    adapter.clear()
                    adapter.addAll(suggestions)
                    adapter.notifyDataSetChanged()
                    if (suggestions.isNotEmpty()) {
                        binding.searchEditText.showDropDown()
                        android.util.Log.d("MapsActivity", "Showing dropdown with ${suggestions.size} suggestions")
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("MapsActivity", "Places search FAILED", exception)
                android.util.Log.e("MapsActivity", "Exception type: ${exception.javaClass.simpleName}")
                android.util.Log.e("MapsActivity", "Exception message: ${exception.message}")
                android.util.Log.e("MapsActivity", "Exception cause: ${exception.cause}")

                runOnUiThread {
                    Toast.makeText(this, "Search failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun selectPlace(position: Int) {
        if (position < placeSuggestions.size) {
            val selectedData = placeSuggestions[position]
            getPlaceDetails(selectedData.prediction.placeId)
            binding.searchEditText.setText(selectedData.displayName)
            binding.searchEditText.visibility = View.GONE
            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
            // Start a new session for the next round of autocomplete per billing guidelines
            newSession()
        }
    }

    private fun getPlaceDetails(placeId: String) {
        android.util.Log.d("MapsActivity", "Getting place details for ID: $placeId")

        val fields = listOf(Place.Field.ID, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(placeId, fields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                android.util.Log.d("MapsActivity", "Place details SUCCESS")
                val place = response.place
                place.latLng?.let { latLng ->
                    android.util.Log.d("MapsActivity", "Moving to location: ${latLng.latitude}, ${latLng.longitude}")
                    runOnUiThread {
                        map.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))
                        map.onMapClick(latLng)
                    }
                } ?: run {
                    android.util.Log.w("MapsActivity", "Place location is null")
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("MapsActivity", "Place details FAILED", exception)
                android.util.Log.e("MapsActivity", "Exception type: ${exception.javaClass.simpleName}")
                android.util.Log.e("MapsActivity", "Exception message: ${exception.message}")
                android.util.Log.e("MapsActivity", "Exception cause: ${exception.cause}")

                runOnUiThread {
                    Toast.makeText(this, "Failed to get place details: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Enable map UI controls
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = true

        map = MapContainer(googleMap, explanationContainer, date) { location ->
            // Callback to store the selected location
            selectedLocation = location
        }
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
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        // Update both LocalDate (month is 0-based in picker) and Calendar instance
        date = LocalDate.of(year, month + 1, dayOfMonth)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        putDateText()
        map.removeMarkers()
        // Restore the previous location if available
        selectedLocation?.let { location ->
            map = MapContainer(map.mMap, explanationContainer, date) { newLocation ->
                // Callback to store the selected location
                selectedLocation = newLocation
            }
            map.onMapClick(location)
        } ?: run {
            // Fallback to device location if no previous location is set
            map = MapContainer(map.mMap, explanationContainer, date) { newLocation ->
                // Callback to store the selected location
                selectedLocation = newLocation
            }
            updateMyLocation()
        }
    }

    private fun putDateText() {
        if (actionBar != null) {
            actionBar?.title = DATE.format(date)
        }

        if (supportActionBar != null) {
            supportActionBar?.title = DATE.format(date)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onDestroy() {
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        super.onDestroy()
    }
}
