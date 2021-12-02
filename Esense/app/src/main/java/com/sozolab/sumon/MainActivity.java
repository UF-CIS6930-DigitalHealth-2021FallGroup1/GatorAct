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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sozolab.sumon.io.esense.esenselib.ESenseManager;
import com.sozolab.sumon.counter.utils.ActivitySubscription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

class ActivityObject {
    private String dateID;
    private int squats = 0;
    private int pushups = 0;
    private int jumpingjacks = 0;
    private int staying = 0;
    private int situps = 0;

    ActivityObject(String dateID) {
        this.dateID = dateID;
    }

    public int getSquats() {
        return squats;
    }

    public void setSquats(int squats) {
        this.squats += squats;
    }

    public int getPushups() {
        return pushups;
    }

    public void setPushups(int pushups) {
        this.pushups += pushups;
    }

    public int getJumpingjacks() {
        return jumpingjacks;
    }

    public void setJumpingjacks(int jumpingjacks) {
        this.jumpingjacks += jumpingjacks;
    }

    public int getSitups() {
        return situps;
    }

    public void setSitups(int situps) {
        this.situps += situps;
    }

    public void setStaying(int staying) {
        this.staying += staying;
    }

    public int getStaying() {
        return this.staying;
    }
}

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
    private Button squatsButton;
    private Button pushupsButton;
    private Button jumpJacksButton;
    private Button sitUpsButton;

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

    private String[] labels;
    private float[] squats;
    private float[] pushups;
    private float[] situps;
    private float[] jumpingjacks;
    private float[] stayings;
    private float[] activityValues;
    private HashMap<String, ActivityObject> mapOfActivities = new HashMap<>();

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

    public static Context getContext(){
        return context_;
    }

    private List<String> getWeekDates() {
        List<String> output = new ArrayList<String>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            output.add("" + cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH) + 1) + cal.get(Calendar.DATE));
            cal.add(Calendar.DATE, -1);
        }
        return output;
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
        squatsButton = (Button) findViewById(R.id.squats_button);
        pushupsButton = (Button) findViewById(R.id.pushups_button);
        jumpJacksButton = (Button) findViewById(R.id.jumpjacks_button);
        sitUpsButton = (Button) findViewById(R.id.situps_button);

        recordButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        headShakeButton.setOnClickListener(this);
        speakingButton.setOnClickListener(this);
        noddingButton.setOnClickListener(this);
        eatingButton.setOnClickListener(this);
        walkButton.setOnClickListener(this);
        stayButton.setOnClickListener(this);
        speakWalkButton.setOnClickListener(this);
        squatsButton.setOnClickListener(this);
        pushupsButton.setOnClickListener(this);
        jumpJacksButton.setOnClickListener(this);
        sitUpsButton.setOnClickListener(this);

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
                System.out.println("date " + tempDate);
                if (documentSnapshot.exists()) {
                    map = documentSnapshot.getData();
                    activityValues = new float[map.size()];
                    activityValues[0] = (Float.parseFloat((String) map.get("SQUATS")));
                    activityValues[1] = (Float.parseFloat((String) map.get("SITUPS")));
                    activityValues[2] = (Float.parseFloat((String) map.get("JUMPING_JACKS")));
                    activityValues[3] = (Float.parseFloat((String) map.get("PUSHUPS")));
                } else {
                    System.out.println("NO FILE");
                }
            }
        });

        // Drop down
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

    private String convertDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);
        return year + "-" + month + "-" + day;
    }

    private void populateValuesFromMap() {
        labels = getWeekDates().toArray(new String[7]);
        squats = new float[7];
        pushups = new float[7];
        situps = new float[7];
        jumpingjacks = new float[7];
        stayings = new float[7];
        int count = 0;
        for (Map.Entry<String, ActivityObject> entry : mapOfActivities.entrySet()) {
            squats[count] = entry.getValue().getSquats();
            pushups[count] = entry.getValue().getPushups();
            situps[count] = entry.getValue().getSitups();
            jumpingjacks[count] = entry.getValue().getJumpingjacks();
            stayings[count] = entry.getValue().getStaying();
            count++;
        }

        // populate unfilled elements with 0
        for (int i = count; i < 7; i++) {
            squats[i] = 0;
            pushups[i] = 0;
            situps[i] = 0;
            jumpingjacks[i] = 0;
            stayings[0] = 0;
        }
    }

    public static boolean handleActivity(String activity) {
        Log.d("handleActivity", "detect Activity - " + activity);

        if (activity != null) {
            String activityLow = activity.toLowerCase();
            if(activityObj != null) {
                Log.d("handleActivity", "chosen activity: " + activityObj.getActivityName().toLowerCase());
            }
            if(activitySummary.get(activityLow) == null) {
                activitySummary.put(activityLow, 0);
            }
            if(activity != "NEUTRAL") {
                int currentCount = activitySummary.get(activityLow) + 1;
                activitySummary.put(activityLow, currentCount);
                Log.d("handleActivity", "counterNum: " + currentCount+ " on counting activity: " + activityLow);
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
            case R.id.view_history:
                List<String> fireStoreDataList = new ArrayList<>();
                // update collections
                db.collection("action").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (mapOfActivities.size() > 0) {
                                mapOfActivities.clear();
                            }

                            List<String> desiredDays = getWeekDates();

                            //Query the database for the current day

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                fireStoreDataList.add(document.getId());
                                System.out.println("document.getId() = " + document.getId());
                                //get value from each document
                                if(!document.getData().containsKey("createTime")) continue;
                                String createTime = convertDate(((Timestamp) document.getData().get("createTime")).toDate());

                                if (!desiredDays.contains(createTime)) {
                                    // Document is not within a  week of current time
                                    continue;
                                }
                                int count = Integer.parseInt(document.getData().get("counterNum").toString());
                                int duration = Integer.parseInt(document.getData().get("duration").toString());
                                String type = document.getData().get("type").toString();

                                System.out.println(createTime + ": " + count + " " + duration + " " + type);

                                if (!mapOfActivities.containsKey(createTime)) {
                                    ActivityObject temp = new ActivityObject(createTime);
                                    switch (type) {
                                        case ("Squats"):
                                            temp.setSquats(count);
                                            break;
                                        case ("SitUps"):
                                            temp.setSitups(count);
                                            break;
                                        case ("PushUps"):
                                            temp.setPushups(count);
                                            break;
                                        case ("Jumping Jacks"):
                                            temp.setJumpingjacks(count);
                                            break;
                                        case ("Staying"):
                                            temp.setStaying(duration);
                                            break;
                                    }
                                    mapOfActivities.put(createTime, temp);
                                } else {
                                    ActivityObject temp = mapOfActivities.get(createTime);
                                    switch (type) {
                                        case ("Squats"):
                                            temp.setSquats(count);
                                            break;
                                        case ("SitUps"):
                                            temp.setSitups(count);
                                            break;
                                        case ("PushUps"):
                                            temp.setPushups(count);
                                        case ("Jumping Jacks"):
                                            temp.setJumpingjacks(count);
                                            break;
                                        case ("Staying"):
                                            temp.setStaying(duration);
                                            break;
                                    }
                                }
                            }
                            Log.d(TAG, fireStoreDataList.toString());
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

                populateValuesFromMap();
                Intent i = new Intent(getApplicationContext(), VisualizeData.class);
                if (labels != null) {
                    i.putExtra("labels", labels);
                    i.putExtra("squats", squats);
                    i.putExtra("situps", situps);
                    i.putExtra("pushups", pushups);
                    i.putExtra("jumpingjacks", jumpingjacks);
                    i.putExtra("stayings", stayings);
                    i.putExtra("values", activityValues);
                }
                startActivity(i);

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

            case R.id.squats_button:
                activityName = "Squats";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.pushups_button:
                activityName = "PushUps";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.jumpjacks_button:
                activityName = "Jumping Jacks";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.situps_button:
                activityName = "SitUps";
                sharedPrefEditor.putString("activityName", activityName);
                sharedPrefEditor.commit();
                setActivityName();
                break;

            case R.id.recordButton:
                if (recordButton.isChecked()) {

                    if (activityName.equals("Activity")) {
                        recordButton.setChecked(false);
                        showAlertMessage();
                        activitySummary.clear();

                    } else {
                        activityObj = new Activity();
                        activitySummary.clear();
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
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
                    SimpleDateFormat fireStoreDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                    String currentDate = dateFormatter.format(currentTime.getTime());
                    String datePrefix = fireStoreDateFormatter.format(currentTime.getTime());
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
                            int totalCount = 0;
                            String chosenAct = activityObj.getActivityName().toLowerCase();
                            if(chosenAct.equals("jumping jacks")) {
                                if (activitySummary.containsKey("squats")) {
                                    totalCount = activitySummary.get("squats");
                                }
                            }
                            else {
                                if (activitySummary.containsKey(chosenAct)) {
                                    totalCount = activitySummary.get(chosenAct);
                                }
                            }
                            activityObj.setCounter(totalCount);

                            databaseHandler.addActivity(activityObj);
                            fireStoreHandler.recordActivity(
                                    activityObj.getActivityName(),
                                    activityObj.getCounter(),
                                    currentDate + " " + activityObj.getStartTime(),
                                    currentDate + " " + activityObj.getStopTime(),
                                    currentTime,
                                    datePrefix
                            );
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
