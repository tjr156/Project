package com.blueproject.kimpc.finalproj;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private EditText txt_temp;
    private EditText txt_humi;
    private EditText txt_dust;
    private Button btn_ok;
    private Button btn_end;

    String temp;
    String humi;
    String dust;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("희망 온습도 및 미세먼지 설정");
        ActionBar actionBar =getSupportActionBar();
        actionBar.hide();

        txt_temp = (EditText)findViewById(R.id.wish_temp);
        txt_humi = (EditText)findViewById(R.id.wish_humi);
        txt_dust = (EditText)findViewById(R.id.wish_dust);
        btn_ok = (Button)findViewById(R.id.btn_ok);
        btn_end = (Button)findViewById(R.id.btn_finish);

        btn_ok.setOnClickListener(this);
        btn_end.setOnClickListener(this);

        temp = txt_temp.getText().toString();
        humi = txt_humi.getText().toString();
        dust = txt_dust.getText().toString();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ok:
                if(txt_temp == null || txt_humi == null || txt_dust == null){
                    Toast.makeText(getApplication(), "희망 환경을 입력하시오.", Toast.LENGTH_SHORT).show();
                    break;
                }
                else{
                    Intent intent = new Intent(MainActivity.this, ViewActivity.class);
                    intent.putExtra("temp", txt_temp.getText().toString());
                    intent.putExtra("humi", txt_humi.getText().toString());
                    intent.putExtra("dust", txt_dust.getText().toString());
                    startActivity(intent);
                    break;
                }



            case R.id.btn_finish:
                finish();
                break;


        }

    }
}