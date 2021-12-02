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
import java.util.List;
import java.util.Map;

public class VisualizeData extends AppCompatActivity {
    private ArrayList barEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualize_data);

        BarChart barChart = findViewById(R.id.BarChart);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            getEntries(extras);
            System.out.println("barEntries.size() = " + barEntries.size());
//            String[] labels = {"SQUATS", "SITUPS", "JUMPING JACKS", "PUSHUPS"};
//            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            Legend legend = barChart.getLegend();
            XAxis xAxis = barChart.getXAxis();
            //change the position of x-axis to the bottom
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);

            Description description = new Description();
            description.setEnabled(false);
            barChart.setDescription(description);
            BarDataSet[] sets = new BarDataSet[7];
            String[] labels = extras.getStringArray("labels");
            // firestore DNS issue
//            float[] squats = extras.getFloatArray("squats");
//            float[] situps = extras.getFloatArray("situps");
//            float[] pushups = extras.getFloatArray("pushups");
//            float[] jacks = extras.getFloatArray("jumpingjacks");
//            float[] stayings = extras.getFloatArray("stayings");

            // hardcode for visualization
            float[] squats = new float[]{10,15,25,30,25, 40, 25};
            float[] situps = new float[]{10,25,10,30,25, 10, 25};
            float[] pushups = new float[]{20,15,20,30,25, 15, 50};
            float[] jacks = new float[]{40,15,25,30,25, 20, 5};
            float[] stayings = new float[]{10,15,25,30,25, 30, 15};


            for (int i = 0; i < 7; i++) {
                List<BarEntry> temp = new ArrayList<>();
                temp.add(new BarEntry(0f+6*i, squats[i]));
                temp.add(new BarEntry(1f+6*i, situps[i]));
                temp.add(new BarEntry(2f+6*i, pushups[i]));
                temp.add(new BarEntry(3f+6*i, jacks[i]));
                temp.add(new BarEntry(4f+6*i, stayings[i]));
                sets[i] = new BarDataSet(temp, labels[i]);
                sets[i].setColors(ColorTemplate.COLORFUL_COLORS);
            }
            BarData barData = new BarData(sets[0], sets[1], sets[2], sets[3], sets[4], sets[5], sets[6]);


//            List<BarEntry> temp = new ArrayList<BarEntry>();
//            temp.add(new BarEntry(0f,10));
//            List<BarEntry> temp1 = new ArrayList<BarEntry>();
//            temp1.add(new BarEntry(1f,10));
//            BarDataSet entry1 = new BarDataSet(temp,"first");
//            entry1.setColor(Color.RED);
//            BarDataSet entry2 = new BarDataSet(temp1, "second");
//            entry2.setColor(Color.BLUE);
//            BarData barData = new BarData(entry1, entry2);
            barChart.setData(barData);
        }
    }

    private BarDataSet[] buildDataSet(Bundle extras) {
        BarDataSet[] sets = new BarDataSet[extras.getStringArray("labels").length];
        for(int i = 0; i < 7; i++) {
            ArrayList<BarEntry> temp = new ArrayList<>();
            BarDataSet barDataSet;
            if (i < extras.getFloatArray("squats").length) {
                temp.add(new BarEntry(i * 1f, extras.getFloatArray("squats")[i]));
                temp.add(new BarEntry((i + 1) * 1f, extras.getFloatArray("pushups")[i]));
                temp.add(new BarEntry((i + 2) * 1f, extras.getFloatArray("situps")[i]));
                temp.add(new BarEntry((i + 3) * 1f, extras.getFloatArray("jumpingjacks")[i]));
                temp.add(new BarEntry((i + 4) * 1f, extras.getFloatArray("stayings")[i]));
                barDataSet = new BarDataSet(temp, extras.getStringArray("labels")[i]);
            }
            else {
                temp.add(new BarEntry(i * 1f, 0));
                temp.add(new BarEntry((i + 1) * 1f, 0));
                temp.add(new BarEntry((i + 2) * 1f, 0));
                temp.add(new BarEntry((i + 3) * 1f, 0));
                temp.add(new BarEntry((i + 4) * 1f, 0));
                barDataSet = new BarDataSet(temp, "no Data");
            }
            sets[i] = barDataSet;
        }
        System.out.println("sets.length = " + sets.length);;
        return sets;
    }


    private void getEntries(Bundle extras) {

        if (extras == null) {
            return;
        }
        barEntries = new ArrayList<BarEntry>();

        for(int i = 0; i < extras.getFloatArray("squats").length; i++) {
            barEntries.add(new BarEntry(i*1f, extras.getFloatArray("squats")[i]));

        }
    }
}