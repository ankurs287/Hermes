package com.godfather.ankur.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GestureDetectorCompat;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.gc.materialdesign.views.ButtonRectangle;
import com.godfather.ankur.travelbuddy.debug.Debug;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeB extends FragmentActivity {
    EditText cityname;
    ButtonRectangle hotel;
    static int PLACE_PICKER_REQUEST = 1;
    LatLng hotelLatLng;

    SparseBooleanArray sparseBooleanArray; // chosen places
    ListView listview;
    ArrayList<LatLng> nodes;
    ArrayList<String> places;
    ButtonRectangle go;
    private Place hotelplace;
    private EditText hc;

    private GestureDetectorCompat gestureDetectorCompat;
    private String ValueHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_b);

        cityname = findViewById(R.id.cityname);
        hotel = findViewById(R.id.hotelbtn);
        go = findViewById(R.id.go);
        listview = findViewById(R.id.listView);
        hc = findViewById(R.id.hc);
        hotelLatLng = null;

        listview.setEnabled(false);
        listview.setVisibility(View.GONE);
        go.setEnabled(false);
        go.setVisibility(View.GONE);
        hc.setVisibility(View.GONE);
        hc.setEnabled(false);

        hotel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(HomeB.this), PLACE_PICKER_REQUEST);
                } catch (Exception e) {
                    Debug.log(e.toString());
                }
            }
        });
    }

    private void getNodes(final MainActivity.GetListener getListener) {
        nodes = new ArrayList<>();
        places = new ArrayList<>();
        nodes.add(hotelLatLng);

        Debug.log("getting Nodes");

        FirebaseFirestore.getInstance().collection(cityname.getText().toString()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Debug.log(document.getId() + " => " + document.getData());
                                HashMap<String, Object> hm = (HashMap<String, Object>) document.getData();
                                String lat = (String) hm.get("lat");
                                String lon = (String) hm.get("long");
                                nodes.add(new LatLng(Double.parseDouble(lat.trim()), Double.parseDouble(lon.trim())));
                                places.add(document.getId().toUpperCase().toString());
                            }
                            getListener.onSuccess(null);

                        } else {
                            Debug.log("Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                hotelplace = PlacePicker.getPlace(this, data);
                hotelLatLng = hotelplace.getLatLng();
                hc.setVisibility(View.VISIBLE);
                hc.setEnabled(false);
                hc.setText(hotelplace.getAddress().toString());

                MainActivity.GetListener getNodesListener = new MainActivity.GetListener() {
                    @Override
                    public void onSuccess(Object data) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                                (HomeB.this,
                                        android.R.layout.simple_list_item_multiple_choice,
                                        android.R.id.text1, places);
                        listview.setAdapter(adapter);
                        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                sparseBooleanArray = listview.getCheckedItemPositions();
                                ValueHolder = "";

                                int i = 0;
                                while (i < sparseBooleanArray.size()) {
                                    if (sparseBooleanArray.valueAt(i)) {
                                        ValueHolder += places.get(sparseBooleanArray.keyAt(i)) + ",";
                                    }
                                    i++;
                                }
                                ValueHolder = ValueHolder.replaceAll("(,)*$", "");
//                                Toast.makeText(HomeB.this, "Selected= " + ValueHolder, Toast.LENGTH_LONG).show();
                            }
                        });
                        listview.setEnabled(true);
                        listview.setVisibility(View.VISIBLE);
                        go.setEnabled(true);
                        go.setVisibility(View.VISIBLE);
                    }
                };
                getNodes(getNodesListener);
            }
        }
    }

    public void go(View view) {
        Intent intent = new Intent(HomeB.this, MainActivity.class);
        Debug.log(hotelLatLng.latitude);
        intent.putExtra("Lat", hotelLatLng.latitude);
        intent.putExtra("Lng", hotelLatLng.longitude);
        intent.putExtra("city", cityname.getText().toString());

        ArrayList<LatLng> nn = new ArrayList<>();
        nn.add(hotelLatLng);
        ArrayList<String> pp = new ArrayList<>();
        pp.add(hotelplace.getAddress().toString());
        int i = 0;
        while (i < sparseBooleanArray.size()) {
            if (sparseBooleanArray.valueAt(i)) {
                nn.add(nodes.get(sparseBooleanArray.keyAt(i)+1));
                pp.add(places.get(sparseBooleanArray.keyAt(i)));
            }
            i++;
        }
        intent.putExtra("nodes", nn);
        intent.putExtra("placesname", pp);
        intent.putExtra("sname", hotelplace.getAddress());
        Debug.log("selected places", ValueHolder);
        Debug.log("send places", pp);
        Debug.log("all places", places);
        Debug.log("all nodes", nodes);
        Debug.log("selected nodes", nn);
        Debug.log(nn.size(), pp.size());
        startActivity(intent);
        finish();
    }


}

