package com.example.ero.deephomeautomation;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class Main2Activity extends AppCompatActivity {

    //StartPage Elements
    TextView compTitle;
    ImageView lines1;
    ImageView lines2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        compTitle=findViewById(R.id.compTag);
        lines1=findViewById(R.id.barlines1);
        lines2=findViewById(R.id.barlines2);

        new CountDownTimer(6000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
                compTitle.animate().alpha(1).setDuration(800);
                lines1.animate().alpha(1).setDuration(1000);
                lines2.animate().alpha(1).setDuration(1000);
            }

            @Override
            public void onFinish() {

                Intent intent = new Intent(Main2Activity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    @Override
    public void onBackPressed() {
        // your stuff here
        //super.onBackPressed();
    }
}
