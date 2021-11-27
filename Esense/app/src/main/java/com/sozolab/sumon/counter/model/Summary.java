package com.sozolab.sumon.counter.model;

import static com.google.firebase.firestore.Query.Direction.DESCENDING;

import android.util.Log;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

public class Summary {
    private String TAG = "Summary";
    private String collectionName = "summaries";
    private Set<String> mIds; //TODO: initialize ids

    private int totalCount; //TODO: initialize totalCount

    private String mId;
    private Date mDate;
    private Map<String, Integer> mCounters;
    private Map<String, Object> docData;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Query collection = FirebaseFirestore.getInstance().collection(collectionName).orderBy("date", DESCENDING);

    public Summary(){
        mDate = new Date();
        mIds = new HashSet<>();
        mCounters = new HashMap<>();
        //create()
        mCounters.put("SITUPS", 0);
        mCounters.put("PUSHUPS", 0);
        mCounters.put("JUMPING_JACKS", 0);
        mCounters.put("SQUATS", 0);

    }

    public Summary(String id, Date date, Map<String, Integer> counters) {
        if (!mIds.contains(id)) {
            mIds.add(id);
        }
        // fromDocument()
        mId = id;
        mDate = date;
        mCounters = counters;
    }

    private void putData(){
        docData = new HashMap<>();
        docData.put("date", mDate);
        docData.put("counters", mCounters);
    }

    // add()
    public Task<Void> add(){
        DocumentReference newDocRef = db.collection(collectionName).document();
        mId = newDocRef.getId();
        putData();
        return newDocRef.set(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "ActivityTransition successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing ActivityTransition", e);
                    }
                });
    }

    // Future<Summary> pull() async {
    //     DocumentSnapshot docRef = await Firestore.instance
    //         .collection(collectionName)
    //         .document(this.id)
    //         .get();
    //     return Summary.fromDocument(docRef);
    // }

    public Task<Void> submit() {
        // create a new summary and save to Firestore if the ID does not exist
        // else update the document
        if (mId == null) {
            add();
        }
        return db.collection(collectionName).document(mId).set(docData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "ActivityTransition successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing ActivityTransition", e);
                    }
                });
    }

    public void reset() {
        Log.d(TAG, "resetting summary for date: " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(mDate));
        for(String key : mCounters.keySet()){
            mCounters.put(key, 0);
        }
    }

    public void increment(String label) {
        mCounters.put(label, mCounters.get(label) + 1);
    }

    public void decrement(String label) {
        mCounters.put(label, mCounters.get(label) - 1);
    }

    public boolean isFromToday() {
        final Date today = new Date();
        ZoneId newYorkZoneId = ZoneId.of("America/New_York"); // ZoneId.systemDefault()
        LocalDate localToday = today.toInstant().atZone(newYorkZoneId).toLocalDate();
        LocalDate localDate = mDate.toInstant().atZone(newYorkZoneId).toLocalDate();
        return (localDate.getYear() == localToday.getYear() &&
                localDate.getMonthValue() == localToday.getMonthValue() &&
                localDate.getDayOfMonth() == localToday.getDayOfMonth());
    }

    String toAccessibleString() {
        String result = "";
        // Map<String, Integer> entries = new HashMap<>();
        for (Map.Entry<String, Integer> exercise : mCounters.entrySet()) {
            if(exercise.getValue() > 0){
                // linkedHashMap.put(employee.getKey(), employee.getValue());
                if(result != ""){
                    result += ", ";
                }
                result += Integer.toString(exercise.getValue()) + " " + exercise.getKey();
            }
        }
        if(result == ""){
            result = "no exercise";
        }
        return result;
    }
}
