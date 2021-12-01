package com.sozolab.sumon;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class FireStoreHandler {
    private  FirebaseFirestore db;
    private static final String TAG = "FireStoreHandler";

    // constructor
    public FireStoreHandler() {
        db = FirebaseFirestore.getInstance();
    }

    public void recordActivity(String actionName, Integer counterNum, String startTime, String stopTime, Calendar currentTime) {
        Map<String, Object> activityObj = new HashMap<>();
        activityObj.put("type", actionName);
        activityObj.put("counterNum", counterNum);
        activityObj.put("createTime", currentTime.getTime());
        activityObj.put("startTime", startTime);
        activityObj.put("stopTime", stopTime);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            Date d1 = sdf.parse(startTime);
            Date d2 = sdf.parse(stopTime);

            // Calucalte time difference
            // in milliseconds
            long differenceInTime = d2.getTime() - d1.getTime();
            long durationInSeconds = (differenceInTime / 1000) % 60;
            activityObj.put("duration", durationInSeconds);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }


        db.collection("action")
            .add(activityObj)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error adding document", e);
                }
            });
    }

    public void test() {
        TimeZone testTimeZone = TimeZone.getTimeZone("UTC");
        Calendar currentTime = Calendar.getInstance(testTimeZone);
        recordActivity("test", 10, "06/05/2020 12:10:10", "06/05/2020 12:10:30", currentTime);
    }
}
