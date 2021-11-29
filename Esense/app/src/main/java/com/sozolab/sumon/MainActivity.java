package com.sozolab.sumon;

import android.graphics.Color;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;

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

import com.sozolab.sumon.io.esense.esenselib.ESenseManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "Esense";
    private String deviceName = "eSense-1625";  // "eSense-0598"
    private String activityName = "Activity";
    private int timeout = 30000;

    private Button connectButton;
    private Button headShakeButton;
    private Button speakingButton;
    private Button noddingButton;
    private Button eatingButton;
    private Button walkButton;
    private Button stayButton;
    private Button speakWalkButton;
    private ListView activityListView;
    private Chronometer chronometer;
    private ToggleButton recordButton;

    private TextView connectionTextView;
    private TextView deviceNameTextView;
    private TextView activityTextView;
    private ImageView statusImageView;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPrefEditor;

    private BarChart barChart;
    private BarData barData;
    private BarDataSet barDataSet;
    private ArrayList barEntries;

    private String selectedDate;
    private Map<String, Map<String, Integer>> dummyDatabase = new HashMap<>();

    Calendar currentTime;
    ESenseManager eSenseManager;
    Activity activityObj;
    Intent audioRecordServiceIntent;
    DatabaseHandler databaseHandler;
    FireStoreHandler fireStoreHandler;
    SensorListenerManager sensorListenerManager;
    ConnectionListenerManager connectionListenerManager;
    private static final int PERMISSION_REQUEST_CODE = 200;

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

        recordButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        headShakeButton.setOnClickListener(this);
        speakingButton.setOnClickListener(this);
        noddingButton.setOnClickListener(this);
        eatingButton.setOnClickListener(this);
        walkButton.setOnClickListener(this);
        stayButton.setOnClickListener(this);
        speakWalkButton.setOnClickListener(this);

        statusImageView = (ImageView) findViewById(R.id.statusImage);
        connectionTextView = (TextView) findViewById(R.id.connectionTV);
        deviceNameTextView = (TextView) findViewById(R.id.deviceNameTV);
        activityTextView = (TextView) findViewById(R.id.activityTV);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chronometer = (Chronometer) findViewById(R.id.chronometer);

        // create firestore instance
        fireStoreHandler = new FireStoreHandler();

        databaseHandler = new DatabaseHandler(this);
        activityListView = (ListView) findViewById(R.id.activityListView);
        ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();
        if (activityHistory.size() > 0) {
            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));
        }

        // BarChart
        barChart = findViewById(R.id.BarChart);
        getEntries();
        barDataSet = new BarDataSet(barEntries, "");
        barData = new BarData(barDataSet);
        barChart.setData(barData);
        barDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(10f);

        // Date Drop Down
        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.dateDropDown);
        //create a list of items for the spinner.

        String[] items = dummyDatabase.keySet().toArray(new String[dummyDatabase.size()]);
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {



            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                // On selecting a spinner item
                selectedDate = dropdown.getSelectedItem().toString();
                System.out.println("Selected " + dropdown.getSelectedItem().toString());

//                barChart.clear();
//                barDataSet.clear();
//                barDataSet.clear();
//
//                barChart = findViewById(R.id.BarChart);
//                getEntries();
//                barData.clearValues();
//
//                barDataSet = new BarDataSet(barEntries, "");
//                barData = new BarData(barDataSet);
//                barChart.setData(barData);
//                barDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
//                barDataSet.setValueTextColor(Color.BLACK);
//                barDataSet.setValueTextSize(10f);

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });



        audioRecordServiceIntent = new Intent(this, AudioRecordService.class);
        sensorListenerManager = new SensorListenerManager(this);
        connectionListenerManager = new ConnectionListenerManager(this, sensorListenerManager,
                connectionTextView, deviceNameTextView, statusImageView, progressBar, sharedPrefEditor);
        eSenseManager = new ESenseManager(deviceName, MainActivity.this.getApplicationContext(), connectionListenerManager);

        if (!checkPermission()) {
            requestPermission();
        } else {
            Log.d(TAG, "Permission already granted..");
        }
    }

    private void getEntries() {
        barEntries = new ArrayList<>();

        // turn Firebase data into bar data

        // Dummy data
        Map<String, Integer> data = new HashMap<>();
        data.put("SITUPS", 2);
        data.put("PUSHUPS", 3);
        data.put("JUMPING_JACKS", 10);
        data.put("SQUATS", 8);
        dummyDatabase.put("Date 1", data);

        Map<String, Integer> data2 = new HashMap<>();
        data2.put("SITUPS", 22);
        data2.put("PUSHUPS", 30);
        data2.put("JUMPING_JACKS", 1);
        data2.put("SQUATS", 12);
        dummyDatabase.put("Date 2", data2);

        Map<String, Integer> data3 = new HashMap<>();
        data3.put("SITUPS", 5);
        data3.put("PUSHUPS", 10);
        data3.put("JUMPING_JACKS", 3);
        data3.put("SQUATS", 4);
        dummyDatabase.put("Date 3", data3);

        Map<String, Integer> display = dummyDatabase.get(selectedDate);
        int count = 1;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            barEntries.add(new BarEntry(2f * count, entry.getValue()));
            count++;
        }

//        barEntries.add(new BarEntry(2f, 0));
//        barEntries.add(new BarEntry(4f, 1));
//        barEntries.add(new BarEntry(6f, 1));
//        barEntries.add(new BarEntry(8f, 3));
//        barEntries.add(new BarEntry(7f, 4));
//        barEntries.add(new BarEntry(3f, 3));
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

                    if (databaseHandler != null) {
                        if (activityObj != null) {
                            databaseHandler.addActivity(activityObj);
                            ArrayList<Activity> activityHistory = databaseHandler.getAllActivities();
                            activityListView.setAdapter(new ActivityListAdapter(this, activityHistory));

                            for (Activity activity : activityHistory) {
                                String activityLog = "Activity : " + activity.getActivityName() + " , Start Time : " + activity.getStartTime()
                                        + " , Stop Time : " + activity.getStopTime() + " , Duration : " + activity.getDuration();
                                Log.d(TAG, activityLog);
                            }
                        }
                    }

                    activityObj = null;
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

    public void connectEarables() {
        eSenseManager.connect(timeout);
    }

    public void startDataCollection(String activity) {
        sensorListenerManager.startDataCollection(activity);
    }

    public void stopDataCollection() {
        sensorListenerManager.stopDataCollection();
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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
