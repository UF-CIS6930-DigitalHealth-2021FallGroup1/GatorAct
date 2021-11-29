package com.sozolab.sumon;
import com.google.firebase.firestore.FirebaseFirestore;

public class Grafana {
    private FirebaseFirestore db;

    Grafana() {
        this.db = FirebaseFirestore.getInstance();

    }

    void testQuery(String name) {
        //
        System.out.println("test = " + this.db.getNamedQuery(name));
    }
}
