package com.blueproject.kimpc.finalproj;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by KimPC on 2017-12-02.
 */

public class IntroActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_start;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_activity);

        btn_start = (Button) findViewById(R.id.btn_start);



        ActionBar actionBar =getSupportActionBar();
        actionBar.hide();


        btn_start.setOnClickListener(this);





    }

    @Override
    public void onClick(View view) {
        Handler handler =new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                startActivity(intent);

                finish();
            }
        }, 0);


    }


}