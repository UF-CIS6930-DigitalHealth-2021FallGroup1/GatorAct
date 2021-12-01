package com.sozolab.sumon;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sozolab.sumon.io.esense.esenselib.ESenseManager;
import com.sozolab.sumon.counter.utils.ActivitySubscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";
    private String deviceName = "eSense-1625";  // "eSense-0598"
    private String activityName = "Activity";

    private int timeout = 60000;

    private Button connectButton;
    private Button headShakeButton;
    private Button speakingButton;
    private Button noddingButton;
    private Button eatingButton;
    private Button walkButton;
    private Button stayButton;
    private Button speakWalkButton;
    private Button counterButton;
    private static ListView activityListView;
    private Chronometer chronometer;
    private ToggleButton recordButton;

    private TextView connectionTextView;
    private TextView deviceNameTextView;
    private static TextView activityTextView;
    private ImageView statusImageView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPrefEditor;

    private float[] activityValues;

//    public FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();

    Calendar currentTime;
    ESenseManager eSenseManager;
    static Activity activityObj;
    Intent audioRecordServiceIntent;
    FirebaseFirestore db;
    FireStoreHandler fireStoreHandler;
    
    static DatabaseHandler databaseHandler;

    SensorListenerManager sensorListenerManager;
    PhoneSensorListenerManager phoneSensorListenerManager;
    ActivitySubscription activitySubscription;
    ConnectionListenerManager connectionListenerManager;
    private static final int PERMISSION_REQUEST_CODE = 200;

    Map<String, Object> map;
    String tempDate = "2021.11.30";
  
    // Adding "Counter" activity
    static HashMap<String,Integer> activitySummary;
    private static Context context_;
    private static Integer counterNum;

    public static Context getContext(){
        return context_;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate()");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.esense);

        sharedPreferences = getSharedPreferences("eSenseSharedPrefs", Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPreferences.edit();

        recordButton = (ToggleButton) findViewById(R.id.recordButton);
        connectButton = (Button) findViewById(R.id.connectButton);
        headShakeButton = (Button) findViewById(R.id.headShakeButton);
        speakingButton = (Button) findViewById(R.id.speakingButton);
        noddingButton = (Button) findViewById(R.id.noddingButton);
        eatingButton = (Button) findViewById(R.id.eatingButton);
        walkButton = (Button) findViewById(R.id.walkButton);
        stayButton = (Button) findViewById(R.id.stayButton);
        speakWalkButton = (Button) findViewById(R.id.speak_walk_button);
        counterButton = (Button) findViewById(R.id.counter_button);

        recordButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        headShakeButton.setOnClickListener(this);
        speakingButton.setOnClickListener(this);
        noddingButton.setOnClickListener(this);
        eatingButton.setOnClickListener(this);
        walkButton.setOnClickListener(this);
        stayButton.setOnClickListener(this);
        speakWalkButton.setOnClickListener(this);
        counterButton.setOnClickListener(this);

        statusImageView = (ImageView) findViewById(R.id.statusImage);
        connectionTextView = (TextView) findViewById(R.id.connectionTV);
        deviceNameTextView = (TextView) findViewById(R.id.deviceNameTV);
        activityTextView = (TextView) findViewById(R.id.activityTV);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chronometer = (Chronometer) findViewById(R.id.chronometer);


        // create firestore instance
        fireStoreHandler = new FireStoreHandler();
        context_ = getApplicationContext();
        databaseHandler = new DatabaseHandler(this);
        activityListView = (ListView) findViewById(R.id.activityListView);
        ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();
        if (activityHistory.size() > 0) {
            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));
        }

        // Firestore collection
        db = FirebaseFirestore.getInstance();
        db.collection("dummyData").document(tempDate).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                System.out.println("date "+tempDate);
                if (documentSnapshot.exists()) {
                    System.out.println("TEST BEGIN");
                    map = documentSnapshot.getData();
                    activityValues = new float[map.size()];
                    activityValues[0] = (Float.parseFloat((String) map.get("SQUATS")));
                    activityValues[1] = (Float.parseFloat((String) map.get("SITUPS")));
                    activityValues[2] = (Float.parseFloat((String) map.get("JUMPING_JACKS")));
                    activityValues[3] = (Float.parseFloat((String) map.get("PUSHUPS")));
                    System.out.println(map.get("SQUATS"));
                    System.out.println("TEST END");
                } else {
                    System.out.println("NO FILE");
                }
            }
        });



        List<String> list = new ArrayList<>();
        String[] items = new String[]{"2021.11.29", "2021.11.30"};
        db.collection("dummyData").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        list.add(document.getId());
                    }
                    Log.d(TAG, list.toString());
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            }
        });

        // Drop down
        Spinner dropdown = findViewById(R.id.dropdown);
//        String[] items = list.toArray(new String[list.size()]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tempDate = dropdown.getSelectedItem().toString();
                Toast.makeText(MainActivity.this, parent.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        findViewById(R.id.visualizeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateActivityValues();
                Intent i = new Intent(getApplicationContext(), VisualizeData.class);
                for (float f : activityValues) {
                    System.out.println(f);
                }
                if (activityValues != null) {
                    System.out.println("activity has values");
                    i.putExtra("values", activityValues);
                }
                startActivity(i);
            }
        });


        audioRecordServiceIntent = new Intent(this, AudioRecordService.class);

        // Adding activity counter
        activitySummary = new HashMap<String, Integer>();
        activitySubscription = new ActivitySubscription(MainActivity::handleActivity);
        sensorListenerManager = new SensorListenerManager(this, activitySubscription);
        phoneSensorListenerManager = new PhoneSensorListenerManager(this, activitySubscription);

        connectionListenerManager = new ConnectionListenerManager(this, sensorListenerManager,
                connectionTextView, deviceNameTextView, statusImageView, progressBar, sharedPrefEditor);
        eSenseManager = new ESenseManager(deviceName, MainActivity.this.getApplicationContext(), connectionListenerManager);

        if (!checkPermission()) {
            requestPermission();
        } else {
            Log.d(TAG, "Permission already granted..");
        }
    }

    private void updateActivityValues() {
        db.collection("dummyData").document(tempDate).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                System.out.println("date "+tempDate);
                if (documentSnapshot.exists()) {
                    System.out.println("TEST BEGIN");
                    map = documentSnapshot.getData();
                    activityValues = new float[map.size()];
                    activityValues[0] = (Float.parseFloat((String) map.get("SQUATS")));
                    activityValues[1] = (Float.parseFloat((String) map.get("SITUPS")));
                    activityValues[2] = (Float.parseFloat((String) map.get("JUMPING_JACKS")));
                    activityValues[3] = (Float.parseFloat((String) map.get("PUSHUPS")));
                    System.out.println(map.get("SQUATS"));
                    System.out.println("TEST END");
                } else {
                    System.out.println("NO FILE");
                }
            }
        });
    }

    public static boolean handleActivity(String activity) {
        Log.d("handleActivity", "Current Activity:" + activity);
        if (activity != null) {
            if(activitySummary.get(activity) == null) {
                activitySummary.put(activity, 0);
            }
            if(activity != "NEUTRAL" && activityObj.getActivityName() == "Counter") {
                int currentCount = activitySummary.get(activity) + 1;
                activitySummary.put(activity, currentCount);
                activityObj.setCounter(currentCount);
                Log.d("handleActivity", "counterNum: " + activityObj.getCounter() + " on counting activity: " + activity);
            }
        };
        return true;
    }

    public static boolean isESenseDeviceConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        this.moveTaskToBack(true);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.clear_menu:
                //Toast.makeText(this, "Clear history...", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.reset_menu:
                //Toast.makeText(this, "Reset connection..", Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.connectButton:
                progressBar.setVisibility(View.VISIBLE);
                connectEarables();
                break;

            case R.id.headShakeButton:
                activityName = "Head Shake";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.speakingButton:
                activityName = "Speaking";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.noddingButton:
                activityName = "Nodding";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.eatingButton:
                activityName = "Eating";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.walkButton:
                activityName = "Walking";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.stayButton:
                activityName = "Staying";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.speak_walk_button:
                activityName = "Speak and Walk";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.counter_button:
                activityName = "Counter";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.recordButton:
                if (recordButton.isChecked()) {

                    if (activityName.equals("Activity")) {
                        recordButton.setChecked(false);
                        showAlertMessage();
                    } else {
                        activityObj = new Activity();

                        currentTime = Calendar.getInstance();
                        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                        int minute = currentTime.get(Calendar.MINUTE);
                        int second = currentTime.get(Calendar.SECOND);

                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.start();

                        if (activityObj != null) {
                            String startTime = hour + " : " + minute + " : " + second;
                            activityObj.setActivityName(activityName);
                            activityObj.setStartTime(startTime);
                        }

                        sharedPrefEditor.putString("checked", "on");
                        sharedPrefEditor.commit();
                        recordButton.setBackgroundResource(R.drawable.stop);

                        audioRecordServiceIntent.putExtra("activity", activityName);

                        startDataCollection(activityName);
                        startService(audioRecordServiceIntent);
                    }

                } else {

                    currentTime = Calendar.getInstance();
                    int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                    int minute = currentTime.get(Calendar.MINUTE);
                    int second = currentTime.get(Calendar.SECOND);

                    chronometer.stop();

                    if (activityObj != null) {
                        String stopTime = hour + " : " + minute + " : " + second;
                        String duration = chronometer.getText().toString();
                        activityObj.setStopTime(stopTime);
                        activityObj.setDuration(duration);
                    }

                    sharedPrefEditor.putString("checked", "off");
                    sharedPrefEditor.commit();
                    recordButton.setBackgroundResource(R.drawable.start);

                    stopDataCollection();
                    stopService(audioRecordServiceIntent);

                    if(databaseHandler != null){
                        if(activityObj != null){
                            databaseHandler.addActivity(activityObj);

                            ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();
                            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));

                            for (Activity activity : activityHistory) {
                                String activityLog = "Activity : " + activity.getActivityName() + " , Start Time : " + activity.getStartTime()
                                        + " , Stop Time : " + activity.getStopTime() + " , Duration : " + activity.getDuration() + ", Counter: " + activity.getCounter();
                                Log.d(TAG, activityLog);
                            }
                        }
                    }

                    activityObj = null;
                    activitySummary.clear();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        progressBar.setVisibility(View.GONE);

        boolean isConnected = isESenseDeviceConnected();
        if (isConnected) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        } else {
            sharedPrefEditor.putString("status", "disconnected");
            sharedPrefEditor.commit();

            activityName = "Activity";
            sharedPrefEditor.putString("activityName", activityName);
            sharedPrefEditor.commit();

            //Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        }

        String isChecked = sharedPreferences.getString("checked", null);
        String status = sharedPreferences.getString("status", null);
        String activity = sharedPreferences.getString("activityName", null);

        if (activity != null) {
            activityName = activity;
            setActivityName();
        }

        if (status == null) {
            connectionTextView.setText("Disconnected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.disconnected);
        } else if (status.equals("connected")) {
            connectionTextView.setText("Connected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.connected);
        } else if (status.equals("disconnected")) {
            connectionTextView.setText("Disconnected");
            deviceNameTextView.setText(deviceName);
            statusImageView.setImageResource(R.drawable.disconnected);
        }

        if (isChecked == null) {
            recordButton.setChecked(false);
            recordButton.setBackgroundResource(R.drawable.start);
        } else if (isChecked.equals("on")) {
            recordButton.setChecked(true);
            recordButton.setBackgroundResource(R.drawable.stop);
        } else if (isChecked.equals("off")) {
            recordButton.setChecked(false);
            recordButton.setBackgroundResource(R.drawable.start);
        }

        Log.d(TAG, "onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    public void setActivityName() {
        activityTextView.setText(activityName);
    }

    public void connectEarables(){
        eSenseManager.connect(timeout);
    }

    public void startDataCollection(String activity) {
        sensorListenerManager.startDataCollection(activity);
        phoneSensorListenerManager.startDataCollection(activity);
    }

    public void stopDataCollection() {
        sensorListenerManager.stopDataCollection();
        phoneSensorListenerManager.stopDataCollection();
    }

    private boolean checkPermission() {
        int recordResult = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        int locationResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int writeResult = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        return locationResult == PackageManager.PERMISSION_GRANTED &&
                writeResult == PackageManager.PERMISSION_GRANTED && recordResult == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean recordAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && storageAccepted && recordAccepted) {
                        Log.d(TAG, "Permission granted");
                    } else {
                        Log.d(TAG, "Permission denied");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel("You need to allow access to all permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{ACCESS_FINE_LOCATION,
                                                            WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void showAlertMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please select an activityName !")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
