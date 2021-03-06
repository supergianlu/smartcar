package smartcity.smartcar.activity;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import smartcity.smartcar.R;
import smartcity.smartcar.UrlConnectionAsyncTask;
import smartcity.smartcar.cluster.MyClusterItem;
import smartcity.smartcar.cluster.ParkingDialogActivity;
import smartcity.smartcar.model.ApplicationService;
import smartcity.smartcar.model.ParkingContent;

import static smartcity.smartcar.model.ParkingContent.ITEMS;

/**
 * Questa activity consente di mostrare una Google Maps inizialmente centrata sulla posizione dell'utente in quel momento.
 * Nel caso che questo abbia dei parcheggi salvati nel database dell'applicazione, allora essi verranno mostrati sotto
 * forma di marker sulla mappa. Cliccando su ognuno di essi sarà possibile visualizzarne i dati.
 * L'ultimo parcheggio verrà mostrato con un'immagine differente e, solo per questo parcheggio,
 * sarà possibile visualizzare il percorso per raggiungere l'autovettura.
 */
public class MapActivity extends MainActivity implements OnMapReadyCallback, UrlConnectionAsyncTask.UrlConnectionListener {

    private GoogleMap mMap;
    private LatLng currentLocation;
    private LatLng carLocation;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setUpMainActivity();
        username = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE).getString("username", "");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapActivity.this, BluetoothActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            recreate();
        }
        if (!mMap.isMyLocationEnabled()) mMap.setMyLocationEnabled(true);

        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15), 1000, null);
                        }
                    }
                });

        try {
            Bundle data = new Bundle();
            data.putString("username", username);
            System.out.println(username);
            new UrlConnectionAsyncTask(new URL(getString(R.string.get_parking)), this, getApplicationContext()).execute(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Questo metodo consente di mostrare sulla mappa tutti i marker relativi ai parcheggi salvati sul database
     */
    private void setAllOldParking() {
        ParkingContent.ParkingItem lastParkingItem = ITEMS.get(0);
        carLocation = new LatLng(lastParkingItem.getLat(), lastParkingItem.getLon());

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {
                if (ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    return;
                }
                final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
                if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                    Toast.makeText(MapActivity.this, "Accendere il GPS", Toast.LENGTH_SHORT).show();
                    return;
                }
                LocationServices.getFusedLocationProviderClient(MapActivity.this).getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                    if(marker.getPosition().equals(carLocation)){
                                        Toast.makeText(MapActivity.this, "Calcolo del percorso...", Toast.LENGTH_SHORT).show();
                                        marker.hideInfoWindow();
                                        setRoute(currentLocation, carLocation);
                                    }
                                }
                            }
                        });
            }
        });

        ClusterManager<ClusterItem> clusterManager = new ClusterManager<>(this, mMap);
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
        for(ParkingContent.ParkingItem parkingItem : ITEMS.subList(1, ITEMS.size())){
            String street;
            Geocoder geocoder = new Geocoder(MapActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(parkingItem.getLat(), parkingItem.getLon(), 1);

                if (!addresses.isEmpty()) {
                    Address returnedAddress = addresses.get(0);
                    street = returnedAddress.getAddressLine(0);
                }
                else {
                    street =  "\""+parkingItem.getLat()+"\",\""+parkingItem.getLon()+"\"";
                }
            } catch (IOException e) {
                e.printStackTrace();
                street =  "\""+parkingItem.getLat()+"\",\""+parkingItem.getLon()+"\"";
            }
            MyClusterItem offsetItem = new MyClusterItem(parkingItem.getLat(), parkingItem.getLon(), "Hai parcheggiato qui il "+parkingItem.getDate()+" alle "+parkingItem.getTime(), street);
            clusterManager.addItem(offsetItem);

            mMap.addMarker(new MarkerOptions().position(carLocation)
                    .title("Hai parcheggiato la macchina qui alle " + lastParkingItem.getTime())
                    .snippet("clicca per visualizzare il percorso")
                    .icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_directions_car))));
        }

        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<ClusterItem>() {
            @Override
            public boolean onClusterClick(Cluster<ClusterItem> cluster) {
                Gson gson = new Gson();
                String clusteCollection = gson.toJson(cluster.getItems());
                Intent intent = new Intent(MapActivity.this, ParkingDialogActivity.class);
                intent.putExtra("clusterCollection", clusteCollection);
                startActivity(intent);
                return true;
            }
        });
    }

    private void setRoute(LatLng origin, LatLng destination){
        GoogleDirection.withServerKey(getString(R.string.google_maps_key_direction))
                .from(origin)
                .to(destination)
                .transitMode(TransportMode.WALKING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
                            mMap.addPolyline(DirectionConverter.createPolyline(MapActivity.this, directionPositionList, 5, Color.parseColor("#2CAAE5")));
                            setCameraWithCoordinationBounds(route);
                        } else {
                            Toast.makeText(MapActivity.this, "Errore nel calcolo del percorso", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Toast.makeText(MapActivity.this, "Errore nel calcolo del percorso", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        ApplicationService.MyBinder b = (ApplicationService.MyBinder) binder;
        service = b.getService();
        if(!service.isRunning()){
            Toast.makeText(this, "La connessione col device è disattivata", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void handleResponse(JSONObject response, Bundle extra) {
        if(response.length() != 0) {
            try {
                final int code = response.getInt("code");
                if(code == 2) {
                    ITEMS.clear();
                    final JSONArray parkingArray = response.getJSONObject("extra").getJSONArray("data");
                    for(int i = 0; i < parkingArray.length(); i++) {
                        JSONObject parking = parkingArray.getJSONObject(i);
                        if(parking.getDouble("lon") != 0) {
                            ParkingContent.addItem(new ParkingContent.ParkingItem(parking.getString("id"), parking.getDouble("lat"), parking.getDouble("lon"), parking.getString("closed"), parking.getString("date")));
                        }
                    }
                }
                if (!ITEMS.isEmpty() && ITEMS.get(0) != null) setAllOldParking();
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Qualcosa è andato storto nell'ottenere i parcheggi dal DB", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
}
