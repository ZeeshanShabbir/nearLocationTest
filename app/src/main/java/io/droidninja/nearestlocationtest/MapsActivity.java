package io.droidninja.nearestlocationtest;

import android.Manifest;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.droidninja.nearestlocationtest.databinding.ActivityMapsBinding;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        EasyPermissions.PermissionCallbacks, View.OnClickListener {

    ActivityMapsBinding binding;

    private static final int RC_LOCATION_PERM = 124;

    private MarkerOptions options = new MarkerOptions();
    private ArrayList<LatLng> latlngs = new ArrayList<>();
    private ArrayList<MarkerOptions> markers = new ArrayList<>();

    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps);
        binding.btnFindNearestLocation.setOnClickListener(this);

        initNearestLocation();

        if (hasLocationAndContactsPermissions()) {
            loadMapFragment();
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.map_location_permission_message),
                    RC_LOCATION_PERM,
                    Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }


    private void initNearestLocation() {
        latlngs.add(new LatLng(-20.164490, 57.501331));
        latlngs.add(new LatLng(-20.166368, 57.500414));
        latlngs.add(new LatLng(-20.167363, 57.506858));
        latlngs.add(new LatLng(-20.158857, 57.502784));
        latlngs.add(new LatLng(-20.157019, 57.508597));
        latlngs.add(new LatLng(-20.153177, 57.504198));
    }


    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void loadMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    private boolean hasLocationAndContactsPermissions() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add a marker in Sydney and move the camera
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14.0f));
            addMarkersForNearestLocations();
            sortListbyDistance(latlngs, new LatLng(location.getLatitude(),
                    location.getLongitude()));
        }
    }

    private void addMarkersForNearestLocations() {
        for (int i = 0; i < latlngs.size(); i++) {
            options.position(latlngs.get(i));
            options.title("Location " + i);
            options.snippet("this is location " + i);
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            mMap.addMarker(options);
            markers.add(options);

        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(getClass().getSimpleName(), "" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == RC_LOCATION_PERM) {
            showAlertDialog(getString(R.string.apologies_title), getString(R.string.no_permission_message), true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Takes lat,lon of two position and returns the distance between them
     *
     * @param firstLatitude
     * @param firstLongitude
     * @param secondLatitude
     * @param secondLongitude
     * @return
     */
    public float calculateDistanceBetweenTwoLocation(double firstLatitude,
                                                     double firstLongitude,
                                                     double secondLatitude,
                                                     double secondLongitude) {
        float[] results = new float[1];
        Location.distanceBetween(firstLatitude, firstLongitude, secondLatitude, secondLongitude, results);
        return results[0];
    }


    public ArrayList<LatLng> sortListbyDistance(ArrayList<LatLng> latLngs, final LatLng location) {
        Collections.sort(latLngs, new Comparator<LatLng>() {
            @Override
            public int compare(LatLng latLng1, LatLng latLng2) {
                if (calculateDistanceBetweenTwoLocation(latLng1.latitude, latLng1.longitude
                        , location.latitude, location.longitude)
                        > calculateDistanceBetweenTwoLocation(latLng2.latitude, latLng2.longitude
                        , location.latitude, location.longitude)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return latLngs;
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_find_nearest_location) {
            if (latlngs != null && latlngs.size() > 0) {
                LatLng latLng = latlngs.get(0);
                String address = getCompleteAddressString(latLng.latitude, latLng.longitude);
                showAlertDialog(getString(R.string.nearest_location_title),
                        getString(R.string.address_message) + " " + address, false);
            }
        }
    }


    private void showAlertDialog(String title, String message, final boolean finishActivity) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .create();
        ad.setCancelable(false);
        ad.setTitle(title);
        ad.setMessage(message);
        ad.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.btn_ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (finishActivity)
                            finish();

                    }
                });
        ad.show();
    }


    private String getCompleteAddressString(double lat, double lon) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strAdd;
    }
}
