package com.example.android.stationfinder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MenuActivity extends Activity {

    @BindView(R.id.btn_rbl)
    Button btn_rbl;

    @BindView(R.id.btn_stations)
    Button btn_stations;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        ButterKnife.bind(this);




    }

    @OnClick(R.id.btn_rbl)
    public void searchByRbls(){

        Intent intent = new Intent(MenuActivity.this, Main2Activity.class);
        startActivity(intent);

    }

    @OnClick(R.id.btn_stations)
    public void searchByName(){

        Intent intent = new Intent(MenuActivity.this, MainActivity.class);
        startActivity(intent);

    }
}
