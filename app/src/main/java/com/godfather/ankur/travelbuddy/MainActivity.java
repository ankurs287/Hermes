package com.godfather.ankur.travelbuddy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.godfather.ankur.travelbuddy.POJO.Example;
import com.godfather.ankur.travelbuddy.debug.Debug;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    ArrayList<LatLng> nodes;
    FirebaseFirestore db;
    LatLng source;
    String cityname;
    RetrofitMaps service;
    long[][] graph;
    int[] optGraph;
    int[] colorGraph;
    int[] optPolyLines;
    boolean[] assigned;
    GetListener getDistanceListener;
    private PolylineOptions[][] polylineGraph;
    ArrayList<String> places;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();

        source = new LatLng(extras.getDouble("Lat"), extras.getDouble("Lng"));
        cityname = extras.getString("city");
        nodes = (ArrayList<LatLng>) extras.getSerializable("nodes");
        places = new ArrayList<>();
//        places.add(extras.getString("sname"));
        places.addAll((ArrayList<String>) extras.getSerializable("placesname"));
        Debug.log("source", source, "cityname", cityname, "nodes", nodes.size());

""
        listView = findViewById(R.id.lv2);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.lvitem, android.R.id.text1, places);
        listView.setAdapter(adapter);


        initCheckAndBuildRetrofit();
        db = FirebaseFirestore.getInstance();

        final GetListener getNodesListener = new GetListener() {
            @Override
            public void onSuccess(Object data) {
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(MainActivity.this);

                graph = new long[nodes.size()][nodes.size()];
                polylineGraph = new PolylineOptions[nodes.size()][nodes.size()];

                Debug.log("Calling optimal path util");
                constructGraph();
            }
        };

        final int[] nodesDone = {0};
        getDistanceListener = new GetListener() {
            @Override
            public void onSuccess(Object d) {
                if (d != null && (int) d == -1) {
                    nodesDone[0]--;
                    return;
                }
                nodesDone[0]++;
                Debug.log("nodes Done:", nodesDone[0]);
                if (nodesDone[0] == nodes.size() * (nodes.size() - 1)) {
                    Debug.log("graph");
                    for (int i = 0; i < nodes.size(); i++) {
                        String yu = "";
                        for (int j = 0; j < nodes.size(); j++) {
                            yu += (" " + graph[i][j]);
                        }
                        Debug.log(yu);
                    }
                    Debug.log("Polylines");
                    for (int i = 0; i < nodes.size(); i++) {
                        String yu = "";
                        for (int j = 0; j < nodes.size(); j++) {
                            yu += (" " + polylineGraph[i][j]);
                        }
                        Debug.log(yu);
                    }
                    optGraph = new int[nodes.size()];
                    optPolyLines = new int[nodes.size()];
                    assigned = new boolean[nodes.size()];
                    getOptimalGraph(0);
                    for (int i = 1; i < nodes.size(); i++) {
                        if (!assigned[i]) {
                            optGraph[i] = 0;
                            break;
                        }
                    }
                    Random rnd = new Random();
                    colorGraph = new int[nodes.size()];
                    Debug.log("g c");
                    for (int i = 0; i < nodes.size(); i++) {
                        int clr = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                        Debug.log(clr);
                        colorGraph[i] = clr;
                        Polyline pll = mMap.addPolyline(polylineGraph[i][optGraph[i]].color(Color.WHITE));
//                        pll.setStartCap(new RoundCap());
//                        pll.setStartCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
//                        pll.setEndCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
                    }

                    String cll = "" + colorGraph[0] + " ";
                    ArrayList<LatLng> polylatlng = new ArrayList<>();
                    polylatlng.add(nodes.get(0));
                    for (int i = optGraph[0]; i != 0; i = optGraph[i]) {
                        polylatlng.add(nodes.get(i));

                        cll += colorGraph[i];
                    }
                    Debug.log("colors ", cll);
                }
            }
        };

        getNodes(getNodesListener);
    }

    private void getOptimalGraph(int u) {
        // fix source
        long temp = Long.MAX_VALUE;
        int temp2 = -1;
        for (int i = 1; i < nodes.size(); i++) {
            if (!assigned[i] && u != i && temp >= graph[u][i]) {
                temp = graph[u][i];
                optGraph[u] = i;
                temp2 = i;
            }
        }

        assigned[u] = true;
        if (temp2 != -1) {
            getOptimalGraph(temp2);
        }
    }

    private void getNodes(final GetListener getListener) {
        if (true) {
            getListener.onSuccess(null);
            return;
        }
        nodes = new ArrayList<>();
        nodes.add(source);

        Debug.log("getting Nodes");
        db.collection(cityname).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Debug.log(document.getId() + " => " + document.getData());
                                HashMap<String, Object> hm = (HashMap<String, Object>) document.getData();
                                String lat = (String) hm.get("lat");
                                String lon = (String) hm.get("long");
                                nodes.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(lon)));
                            }
                            getListener.onSuccess(null);

                        } else {
                            Debug.log("Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void constructGraph() {
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = 0; j < nodes.size(); j++) {
                if (i != j)
                    getDistance(i, j, "driving");
            }
        }
    }

    private void getDistance(final int u, final int v, String type) {
        final LatLng origin = nodes.get(u);
        final LatLng dest = nodes.get(v);
        Call<Example> call = service.getDistanceDuration("metric", origin.latitude + "," + origin.longitude, dest.latitude + "," + dest.longitude, type);

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Response<Example> response, Retrofit retrofit) {
                try {
                    // This loop will go through all the results and add marker on each location.
                    Debug.log("routes", response.body().getRoutes().size());
                    long t = Long.MAX_VALUE;
                    PolylineOptions pl = null;
                    for (int i = 0; i < response.body().getRoutes().size(); i++) {
                        String distance = response.body().getRoutes().get(i).getLegs().get(i).getDistance().getText();
                        long time = response.body().getRoutes().get(i).getLegs().get(i).getDuration().getValue();
                        Debug.log("Distance:" + distance + ", Duration:" + time);
                        String encodedString = response.body().getRoutes().get(0).getOverviewPolyline().getPoints();
                        List<LatLng> list = decodePoly(encodedString);
//                        line = mMap.addPolyline(new PolylineOptions()
//                                .addAll(list)
//                                .width(10)
//                                .color(Color.RED)
//                                .geodesic(true)
//                        );
                        if (time <= t) {
                            t = time;
                            pl = new PolylineOptions()
                                    .addAll(list)
                                    .width(8)
                                    .geodesic(true);
                        }
                    }
                    graph[u][v] = t;
                    polylineGraph[u][v] = pl;
                    getDistanceListener.onSuccess(null);
                } catch (Exception e) {
                    getDistanceListener.onSuccess((int) -1);
                    Debug.log("onResponse", "There is an error", e.toString(), e.getStackTrace(), e.getCause(), e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Debug.log("onFailure", t.toString());
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.map_style);
        mMap.setMapStyle(style);

        mMap.addMarker(new MarkerOptions().position(nodes.get(0)).title(places.get(0)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        for (int i = 1; i < nodes.size(); i++) {
            mMap.addMarker(new MarkerOptions().position(nodes.get(i)).title(places.get(i)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(source));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
    }

    private void initCheckAndBuildRetrofit() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Debug.log("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        } else {
            Debug.log("onCreate", "Google Play Services available. Continuing.");
        }

        String url = "https://maps.googleapis.com/maps/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(RetrofitMaps.class);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    interface GetListener {
        void onSuccess(Object data);
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

}
