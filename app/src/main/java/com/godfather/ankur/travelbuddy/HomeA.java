package com.godfather.ankur.travelbuddy;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class HomeA extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_a);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(HomeA.this));
        recyclerView.addItemDecoration(new DividerItemDecoration(HomeA.this, DividerItemDecoration.VERTICAL_LIST));

        RecyclerViewAdapterCountry adapter = new RecyclerViewAdapterCountry(HomeA.this, getResources()
                .getStringArray(R.array.countries));
        recyclerView.setAdapter(adapter);
    }
}
