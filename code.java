package ngo.sapne.intents.sapne;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import ngo.sapne.intents.sapne.R;


public class NearbyNGO extends android.support.v4.app.Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

 ArrayList<String> listItems;
 ArrayList<Double> nearbylocation;

 ArrayAdapter<String> adapter;
 HashMap<Double, List<String>> map;
 private ListView mListView;
 private MapView mapView;
 public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
 private GoogleMap mMap;
 LocationManager manager;
 LocationManager locationManager;
 private DatabaseReference mDatabase;
 Double latitude;
 Double longitude;
 String TAG = "blood";
 //gps location part
 private GoogleApiClient mGoogleApiClient;
 private Location mLocation;
 private LocationManager locationManacityNameger;
 private LocationRequest mLocationRequest;
 private com.google.android.gms.location.LocationListener listener;
 private long UPDATE_INTERVAL = 2 * 1000; /* 10 sec */
 private long FASTEST_INTERVAL = 2000; /* 2 sec */
 int PROXIMITY_RADIUS = 20000;


 @Override
 public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
 View v = inflater.inflate(R.layout.activity_nearby_ngo, container, false);
 mListView = v.findViewById(R.id.addressList);

 checkLocationPermission();

 map = new HashMap<>();
 listItems = new ArrayList<String>();
 nearbylocation = new ArrayList<Double>();

 manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
 mDatabase = FirebaseDatabase.getInstance().getReference().child("NearByNgo");


 latitude = 0.0;
 longitude = 0.0;
 mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
 .addConnectionCallbacks(this)
 .addOnConnectionFailedListener(this)
 .addApi(LocationServices.API)
 .build();

 locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);


 // Obtain the SupportMapFragment and get notified when the map is ready to be used.
 /* SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager()
 .findFragmentById(R.id.map);
 mapFragment.getMapAsync(this);*/
 mapView = (MapView) v.findViewById(R.id.map);
 mapView.onCreate(savedInstanceState);
 mapView.onResume();
 mapView.getMapAsync(this);//when you already implement OnMapReadyCallback in your fragment

 adapter = new ArrayAdapter<String>(getActivity(),
 android.R.layout.simple_list_item_1,
 listItems);
 mListView.setAdapter(adapter);
 //Required later for opening description for each List Item

 mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
 @Override
 public void onItemClick(AdapterView<?> parent, View view, int position,
 long id) {
 Double latitude = Double.parseDouble(map.get(nearbylocation.get(position)).get(1)); //getting the position. Then using postion, we are getting distance stored. Using distance as key, we are getting latitude as value
 Double longitude = Double.parseDouble(map.get(nearbylocation.get(position)).get(2)); //getting the position. Then using postion, we are getting distance stored. Using distance as key, we are getting longitude as value
 String name = map.get(nearbylocation.get(position)).get(0); //getting the position. Then using postion, we are getting distance stored. Using distance as key, we are getting name as value.
 showNGO(name, latitude, longitude);

 }
 });

 return v;
 }

 private void sortlocation(Double lat, Double longi, String ngoname) {

 double distance = distance(lat, this.latitude, longi, this.longitude, 0.0, 0.0);

 nearbylocation.add(distance);
 Collections.sort(nearbylocation);


 map.put(distance, new ArrayList<String>());
 map.get(distance).add(ngoname);
 map.get(distance).add(Double.toString(lat));
 map.get(distance).add(Double.toString(longi));

 }

 Handler waitMsgHandler = new Handler() {
 @Override
 public void handleMessage(Message msg) {

 if (!(latitude != null || latitude == 0.0) || !(longitude == null || longitude == 0.0)) {
 LatLng mylocation = new LatLng(latitude, longitude);
 mMap.moveCamera(CameraUpdateFactory.newLatLng(mylocation));

 mMap.addMarker(new MarkerOptions().position(mylocation).title("My Location"));
 mMap.addMarker(new MarkerOptions().position(mylocation).icon(BitmapDescriptorFactory.fromAsset("circle.png")));
 mMap.setMinZoomPreference(11);
 }

 retrieveDatabase();


 }
 };

 private void retrieveDatabase() {
 mDatabase.addValueEventListener(new ValueEventListener() {
 Double latitude, longitude;

 @Override
 public void onDataChange(DataSnapshot snapshot) {
 try {
 for (DataSnapshot ngoname : snapshot.getChildren()) {

 for (DataSnapshot latlng : ngoname.getChildren()) {
 if (latitude == null) {
 latitude = Double.parseDouble(latlng.getValue().toString());
 } else {
 longitude = Double.parseDouble(latlng.getValue().toString());
 }
 }
 sortlocation(latitude, longitude, ngoname.getKey());
 latitude = null;
 longitude = null;
 }
 } catch (Exception e) {
 Log.e("exception", "Catch in database " + e.getMessage());
 }

 updateList();
 }

 @Override
 public void onCancelled(DatabaseError databaseError) {
 Log.e("exception", "database error =" + databaseError.getMessage());
 }
 });
 }

 private void updateList() {
 for (Double nearbyNGODistance : nearbylocation) {
 String name = map.get(nearbyNGODistance).get(0);

 listItems.add(name + " is " + String.format("%.2f", nearbyNGODistance) + " KM away");

 }
 mListView.invalidateViews();
 }

 private void showNGO(String placename, Double latitude, Double longitude) {

 MarkerOptions markerOptions = new MarkerOptions();


 double lat = latitude;
 double lng = longitude;


 LatLng latLng = new LatLng(lat, lng);
 markerOptions.position(latLng);
 markerOptions.title(placename);


 mMap.addMarker(markerOptions);
 mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
 mMap.animateCamera(CameraUpdateFactory.zoomTo(5));

 }

 public static double distance(double lat1, double lat2, double lon1,
 double lon2, double el1, double el2) {

 final int R = 6371; // Radius of the earth

 double latDistance = Math.toRadians(lat2 - lat1);
 double lonDistance = Math.toRadians(lon2 - lon1);
 double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
 double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
 double distance = R * c * 1000; // convert to meters

 double height = el1 - el2;

 distance = Math.pow(distance, 2) + Math.pow(height, 2);

 return (Math.sqrt(distance) / 1000);
 }


 public boolean checkLocationPermission() {
 if (ContextCompat.checkSelfPermission(getActivity(),
 android.Manifest.permission.ACCESS_FINE_LOCATION)
 != PackageManager.PERMISSION_GRANTED) {

 // Asking user if explanation is needed
 if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
 android.Manifest.permission.ACCESS_FINE_LOCATION)) {

 // Show an expanation to the user *asynchronously* -- don't block
 // this thread waiting for the user's response! After the user
 // sees the explanation, try again to request the permission.

 //Prompt the user once explanation has been shown
 ActivityCompat.requestPermissions(getActivity(),
 new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
 MY_PERMISSIONS_REQUEST_LOCATION);


 } else {
 // No explanation needed, we can request the permission.
 ActivityCompat.requestPermissions(getActivity(),
 new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
 MY_PERMISSIONS_REQUEST_LOCATION);
 }
 return false;
 } else {
 return true;
 }
 }
 @Override
 public void onRequestPermissionsResult(int requestCode,
 String permissions[], int[] grantResults) {
 switch (requestCode) {
 case MY_PERMISSIONS_REQUEST_LOCATION: {
 // If request is cancelled, the result arrays are empty.
 if (grantResults.length > 0
 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

 // Permission was granted.
 if (ContextCompat.checkSelfPermission(getActivity(),
 Manifest.permission.ACCESS_FINE_LOCATION)
 == PackageManager.PERMISSION_GRANTED) {


 }

 } else {

 // Permission denied, Disable the functionality that depends on this permission.
 Toast.makeText(getActivity(), "Permission denied", Toast.LENGTH_LONG).show();
 }
 return;
 }

 // other 'case' lines to check for other permissions this app might request.
 //You can add here other case statements according to your requirement.
 }
 }
 /**
 * Manipulates the map once available.
 * This callback is triggered when the map is ready to be used.
 * This is where we can add markers or lines, add listeners or move the camera. In this case,
 * we just add a marker near SydnemapView = (MapView) view.findViewById(R.id.map);
 * mapView.onCreate(savedInstanceState);
 * mapView.onResume();
 * mapView.getMapAsync(this);//when you already implement OnMapReadyCallback in your fragmenty, Australia.
 * If Google Play services is not installed on the device, the user will be prompted to install
 * it inside the SupportMapFragment. This method will only be triggered once the user has
 * installed Google Play services and returned to the app.
 */
 @Override
 public void onMapReady(GoogleMap googleMap) {
 if ((latitude == null || latitude == 0.0) || (longitude == null || longitude == 0.0)) {


 Runnable r = new Runnable() {
 @Override
 public void run() {
 // What do you want the thread to do


 while ((latitude == null || latitude == 0.0) || (longitude == null || longitude == 0.0)) {

 synchronized (this) {
 try {
 Log.i(TAG, "Thread");

 } catch (Exception e) {
 }
 }
 }

 waitMsgHandler.sendEmptyMessage(0);
 }
 };

 Thread waitThread = new Thread(r);
 waitThread.start();
 mMap = googleMap;


 }
 }

 public void onConnected(Bundle bundle) {
 if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
 // TODO: Consider calling
 // ActivityCompat#requestPermissions
 // here to request the missing permissions, and then overriding
 // public void onRequestPermissionsResult(int requestCode, String[] permissions,
 // int[] grantResults)
 // to handle the case where the user grants the permission. See the documentation
 // for ActivityCompat#requestPermissions for more details.
 return;
 }
 startLocationUpdates();
 mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
 if (mLocation == null) {
 startLocationUpdates();
 }
 if (mLocation != null) {

 latitude = mLocation.getLatitude();
 longitude = mLocation.getLongitude();
 /*this.longitude = Double.toString(longitude);
 this.latitude = Double.toString(latitude);*/


 } else {
 Toast.makeText(getActivity(), "Location not Detected", Toast.LENGTH_SHORT).show();
 }

 }


 protected void startLocationUpdates() {
 // Create the location request
 mLocationRequest = LocationRequest.create()
 .setPriority(LocationRequest.PRIORITY_LOW_POWER)
 .setInterval(UPDATE_INTERVAL)
 .setFastestInterval(FASTEST_INTERVAL);
 // Request location updates
 if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
 // TODO: Consider calling
 // ActivityCompat#requestPermissions
 // here to request the missing permissions, and then overriding
 // public void onRequestPermissionsResult(int requestCode, String[] permissions,
 // int[] grantResults)
 // to handle the case where the user grants the permission. See the documentation
 // for ActivityCompat#requestPermissions for more details.
 return;
 }
 LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
 mLocationRequest, this);
 Log.d("reque", "--->>>>");
 }

 @Override
 public void onConnectionSuspended(int i) {
 Log.i(TAG, "Connection Suspended");
 mGoogleApiClient.connect();
 }


 @Override
 public void onStart() {
 super.onStart();
 mGoogleApiClient.connect();
 }

 @Override
 public void onStop() {
 super.onStop();
 if (mGoogleApiClient.isConnected()) {
 mGoogleApiClient.disconnect();
 }
 }


 public void addAddress(String address) {
 adapter.add(address);
 }


 @Override
 public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

 }

 @Override
 public void onLocationChanged(Location location) {

 }


}
