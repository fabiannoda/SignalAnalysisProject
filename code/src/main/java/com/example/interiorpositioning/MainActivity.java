package com.example.interiorpositioning;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.interiorpositioning.entidades.data;
import com.example.interiorpositioning.utilidades.utilidades;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    DatabaseReference database;

    SensorManager sensorManager;
    Sensor sensor;
    SensorEventListener sensorEventListener;

    boolean running = false;
    Button siguiente;

    public int tiempo;
    public final Handler handler=new Handler();

    private String acelera;
    private String giro;
    private String magneto;
    private String wifi;
    private String pasos="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        siguiente = (Button) findViewById(R.id.btnvis);
        siguiente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent siguiente= new Intent(MainActivity.this, Visualizar.class);
                startActivity(siguiente);
            }
        });
        ConexionSQLiteHelper conn=new ConexionSQLiteHelper(this,"bd_data",null,1);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acele();
        giros();
        magne();
        wifi();

    }

    // Aqui se Encuentran todas las funciones relacionadas con el Acelerometro!!

    private String acele(){
        sensorManager =  (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_ACCELEROMETER);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                acelera="x: "+x+"\n                            y: "+y+"\n                            z: "+z;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("Acelerometro no se mueve");
            }
        };
        start();
        return acelera;


    }

    // Aqui se Encuentran todas las funciones relacionadas con el Giroscopio!!
    private String giros(){

        sensorManager =  (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_GYROSCOPE);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                giro="x: "+x+"\n                        y: "+y+"\n                        z: "+z;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("Giroscopio no se mueve");
            }
        };
        start();
        return giro;
    }
    // Aqui se Encuentran todas las funciones relacionadas con el Magnetómetro!!

    private String magne(){

        sensorManager =  (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_MAGNETIC_FIELD);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                magneto="x: "+x+"\n                               y: "+y+"\n                               z: "+z;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                System.out.println("Magnetometro no se mueve");
            }
        };
        start();
        return magneto;
    }

    //The basic answer to this question is that you cannot get it via the Android API.
    // You might be able to do it for individual chips by modifying the Android firmware or hooking into a device specific driver.
    // However, you should be aware that most wifi cards do not report SNR! (link..)
    // As such, there is certainly no way to obtain SNR through the Android SDK classes,
    // and there may not be any way even if you do have the ability to modify the firmware (you don't, for all practical purposes).
    // I dug through all of the Android SDK specific android.net.wifi sources and didn't find anything supporting this.
    private String wifi(){
        WifiManager info=(WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiinfo=info.getConnectionInfo();
        wifi=String.valueOf(wifiinfo.getRssi());
        wifi=wifi+" dB";
        return wifi;
    }

    public void insert(){
        ConexionSQLiteHelper conn=new ConexionSQLiteHelper(this,"bd_data",null,1);

        SQLiteDatabase db=conn.getWritableDatabase();

        data mdata = new data(new Date().toString(), wifi(), magne(),acele(),giros(),pasos);

        ContentValues values=new ContentValues();
        values.put(utilidades.campo_acele,mdata.getAcelerometro());
        values.put(utilidades.campo_fecha,mdata.getFecha());
        values.put(utilidades.campo_giro,mdata.getGiroscopio());
        values.put(utilidades.campo_magneto,mdata.getMagneto());
        values.put(utilidades.campo_mac,mdata.getMac());
        values.put(utilidades.campo_pasos,mdata.getPasos());



        Long idResultado=db.insert(utilidades.tabla_data,utilidades.campo_fecha,values);
        Toast.makeText(getApplicationContext(),"Id Registro "+idResultado,Toast.LENGTH_SHORT).show();
        db.close();



        database = FirebaseDatabase.getInstance().getReference();

        String s ="Sera que ahora yes?";

        Map<String, String> datosSensores = new HashMap<>();
        datosSensores.put(utilidades.campo_acele,mdata.getAcelerometro());
        datosSensores.put(utilidades.campo_fecha,mdata.getFecha());
        datosSensores.put(utilidades.campo_giro,mdata.getGiroscopio());
        datosSensores.put(utilidades.campo_magneto,mdata.getMagneto());
        datosSensores.put(utilidades.campo_mac,mdata.getMac());
        datosSensores.put(utilidades.campo_pasos,mdata.getPasos());

        database.child("Sensores").push().setValue(datosSensores);

    }


    // Aqui se encuentran los metodos del ciclo de vida de la actividad!!


    @Override
    protected void onPause() {
        stop();
        stopPasos();
        super.onPause();
    }
    @Override
    protected void onResume() {
        start();
        startPasos();
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //Aqui se encuentran los metodos para ejecutar los eventos!!

    private void start(){
        running = true;
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stop(){
        running = false;
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(running){
            pasos=String.valueOf(event.values[0]);
            insert();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    private void startPasos() {
        running = true;
        Sensor count = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (count != null) {
            sensorManager.registerListener((SensorEventListener) this, count, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Sensor de pasos no disponible :(", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPasos() {
        running = false;
    }

    private void ejecutar(){

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                insert();
                handler.postDelayed(this, tiempo);
            }
        }, tiempo);
    }

    public void insert(View view) {
        insert();
    }
     public void tempo(View v){
         EditText text =(EditText)findViewById(R.id.texto);
         tiempo=Integer.parseInt(text.getText().toString())*1000;
         Button boton=(Button) findViewById(R.id.button2);
         boton.setEnabled(false);
         ejecutar();
     }
}
