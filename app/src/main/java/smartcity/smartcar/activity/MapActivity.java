package smartcity.smartcar.activity;


import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
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
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import smartcity.smartcar.R;
import smartcity.smartcar.cluster.MyClusterItem;
import smartcity.smartcar.cluster.ParkingDialogActivity;
import smartcity.smartcar.model.ApplicationService;
import smartcity.smartcar.model.ParkingContent;
import smartcity.smartcar.utility.Helper;

import static smartcity.smartcar.model.ParkingContent.ITEMS;

public class MapActivity extends MainActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng currentLocation;
    private LatLng carLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        setUpMainActivity();
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
            ActivityCompat.requestPermissions(MapActivity.this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if(!mMap.isMyLocationEnabled()) mMap.setMyLocationEnabled(true);

        LocationServices.getFusedLocationProviderClient(this).getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            if(ITEMS.get(0) != null) setAllOldParking();
                        }
                    }
                });
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void setAllOldParking() {
        ParkingContent.ParkingItem lastParkingItem = ITEMS.get(0);
        carLocation = new LatLng(lastParkingItem.getLat(), lastParkingItem.getLon());
        mMap.addMarker(new MarkerOptions().position(carLocation)
                .title("Hai parcheggiato la macchina qui alle "+lastParkingItem.getTime())
                .snippet("clicca per visualizzare il percorso")
                .icon(getMarkerIconFromDrawable(getResources().getDrawable(R.drawable.ic_directions_car))));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15), 1000, null);
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if(marker.getPosition().equals(carLocation)){
                    Toast.makeText(MapActivity.this, "Calcolo del percorso...", Toast.LENGTH_SHORT).show();
                    marker.hideInfoWindow();
                    setRoute(currentLocation, carLocation);
                }
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

}
