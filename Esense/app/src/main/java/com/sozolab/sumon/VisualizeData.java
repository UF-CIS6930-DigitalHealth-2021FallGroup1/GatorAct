package com.sozolab.sumon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class VisualizeData extends AppCompatActivity {
    private ArrayList barEntries;
    String tempDate = "2021.11.30";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualize_data);

        BarChart barChart = findViewById(R.id.BarChart);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            getEntries(extras.getFloatArray("values"));
            System.out.println("barEntries.size() = " + barEntries.size());
            String[] labels = {"SQUATS", "SITUPS", "JUMPING JACKS", "PUSHUPS"};
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            Legend legend = barChart.getLegend();
            XAxis xAxis = barChart.getXAxis();
            //change the position of x-axis to the bottom
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            xAxis.setGranularity(1f);

            Description description = new Description();
            description.setEnabled(false);
            barChart.setDescription(description);

            BarDataSet barDataSet = new BarDataSet(barEntries, "Activities");
            BarData barData = new BarData(barDataSet);
            barChart.setData(barData);
            barDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
            barDataSet.setValueTextColor(Color.BLACK);
            barDataSet.setValueTextSize(10f);
        }
    }



    private void getEntries(float[] date) {

        if (date == null) {
            return;
        }
        barEntries = new ArrayList<BarEntry>();
        barEntries.add(new BarEntry(0, date[0]));
        barEntries.add(new BarEntry(1f, date[1]));
        barEntries.add(new BarEntry(2f, date[2]));
        barEntries.add(new BarEntry(3f, date[3]));
        // doesn't quite work
    }
//        db.collection("dummyData")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.d(TAG, document.getId() + " => " + document.getData());
//                                Map<String, Object> dataHash = document.getData();
//
//                                int count = 1;
//                                for (Map.Entry<String, Object> entry : dataHash.entrySet()) {
//                                    System.out.println(entry.getKey() + " = " + entry.getValue());
////                                    barEntries.add(new BarEntry(2f * count, count * 3f));
//                                    count++;
//                                }
//                            }
//                        } else {
//                            Log.w(TAG, "Error getting documents.", task.getException());
//                        }
//                    }
//                });
//        Map<String, Integer> display = dummyDatabase.get(selectedDate);
//        int count = 1;
//        for (Map.Entry<String, Integer> entry : data.entrySet()) {
//            barEntries.add(new BarEntry(2f * count, entry.getValue()));
//            count++;
//        }
//        barEntries.add(new BarEntry(0, 10));
//    }
}