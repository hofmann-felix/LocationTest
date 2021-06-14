package com.example.locationtest

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.GsonBuilder
import okhttp3.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val LOCATION_PERMISSION_REQ_CODE = 1000
    private var latitude: Double? = null
    private var longitude: Double? = null
    private lateinit var map: MapView
    private lateinit var standortBestaetigen: Button
    private lateinit var radiobtns: RadioGroup
    private val client = OkHttpClient()
    lateinit var mvgResponse: MVGResponse
    var test: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)
        standortBestaetigen = findViewById(R.id.select_location_btn)
        radiobtns = findViewById(R.id.radiobtns)


        //Map erstellen
        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        //Map auf München setzen
        val mapController = map.controller
        mapController.setZoom(12.5)
        val startPoint = GeoPoint(48.13748329597889, 11.575290183617394);
        mapController.setCenter(startPoint);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        standortBestaetigen.setOnClickListener {
            getCurrentLocation()
            getNearestStations()

        }

    }

    private fun getNearestStations() {
        val MVGUrl =
            "https://www.mvg.de/api/fahrinfo/location/nearby?latitude=${latitude.toString()}&longitude=${longitude.toString()}"
        val request = Request.Builder()
            .url(MVGUrl)
            .build()

//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {}
//            override fun onResponse(call: Call, response: Response) =
//                setResponse(response.body()?.string())
//        })

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.string()
                print(body)

                val gson = GsonBuilder().create()

                mvgResponse = gson.fromJson(body, MVGResponse::class.java)
                updateRadioButtons()

            }
            override fun onFailure(call: Call, e: IOException) {}
        })
    }

    private fun updateRadioButtons() {

            runOnUiThread {
                for(station in mvgResponse.locations){
                    val radioBtn = RadioButton(this)
                    radioBtn.text = station.name
                    radiobtns.addView(radioBtn)
                }
                standortBestaetigen.visibility = View.INVISIBLE
                radiobtns.visibility = View.VISIBLE
            }

    }


    override fun onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE
            );
            Toast.makeText(this, "Missing permission", Toast.LENGTH_SHORT).show()

            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                latitude = location?.latitude
                longitude = location?.longitude
                Toast.makeText(
                    this,
                    "Latitude: ${latitude.toString()} Longitude: ${longitude.toString()} ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this, "Failed on getting current location",
                    Toast.LENGTH_SHORT
                ).show()
            }

    }
}

class MVGResponse(val locations: List<Station>)
class Station(val name: String, val distance: Int, val products: List<String>)