package com.example.escapetheearth;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    DrawView mainframe;
    EditText angInput; EditText vInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainframe = (DrawView) findViewById(R.id.mainframe);
        angInput = (EditText) findViewById(R.id.angleInput);
        vInput = (EditText) findViewById(R.id.velocityInput);
    }

    public void startClick(View v){
        mainframe.startClick();
    }

    public void resetClick(View v){
        mainframe.resetClick();
    }

    public void updateAngle(View v){
        double newAng = Double.parseDouble(angInput.getText().toString());
        mainframe.updateAngle(newAng);
    }

    public void updateVelocity(View v){
        double newV = Double.parseDouble(vInput.getText().toString());
        if(newV < 500 || newV > 1400) {
            Toast.makeText(getApplicationContext(), "Enter velocity between 500 and 1400!", Toast.LENGTH_SHORT).show();
        }
        else
            mainframe.updateVelocity(newV);
    }
}