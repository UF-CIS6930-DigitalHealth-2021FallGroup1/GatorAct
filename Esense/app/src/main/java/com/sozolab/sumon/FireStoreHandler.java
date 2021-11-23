package com.sozolab.sumon;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FireStoreHandler {
    private  FirebaseFirestore db;
    private static final String TAG = "FireStoreHandler";
    
    // constructor
    public FireStoreHandler() {
        db = FirebaseFirestore.getInstance();
    }

    public void test() {
        Map<String, Object> test = new HashMap<>();
        test.put("first", "Ada");
        test.put("last", "Lovelace");
        test.put("born", 1815);

        // Add a new document with a generated ID
        db.collection("action")
                .add(test)
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
}
