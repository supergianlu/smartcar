package smartcity.smartcar.activity;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;

import smartcity.smartcar.R;
import smartcity.smartcar.model.ParkingContent;

import static smartcity.smartcar.model.ParkingContent.ITEMS;

/**
 * Questa Activity contiene un grafico a torta che mostra le percentuali e il numero effettivo di parcheggi
 * in cui l'utente si è allontanato dalla macchina dimenticandola aperta o lasciandola correttamente chiusa
 */
public class ChartActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        setUpMainActivity();

        int closed = 0;
        int opened = 0;
        for(ParkingContent.ParkingItem parkingItem: ITEMS){
            if(parkingItem.isClosed()){
                closed++;
            } else {
                opened++;
            }
        }
       float closedPercent = closed*(opened+closed)/100f;
       float openedPercent = opened*(opened+closed)/100f;

       PieChart pieChart = findViewById(R.id.pieChart);
       pieChart.setUsePercentValues(true);

       ArrayList<Entry> entries = new ArrayList<>();
       entries.add(new Entry(closedPercent, 0));
       entries.add(new Entry(openedPercent, 1));

       PieDataSet dataSet = new PieDataSet(entries, "");

       ArrayList<String> labels = new ArrayList<>();
       labels.add("Chiusa "+closed+" volte");
       labels.add("Non chiusa "+opened+" volte");

       PieData data = new PieData(labels, dataSet);
       data.setValueFormatter(new PercentFormatter());
       pieChart.setData(data);
       pieChart.setDescription("");

       pieChart.setDrawHoleEnabled(false);
       pieChart.setTransparentCircleRadius(25f);
       pieChart.setHoleRadius(25f);

       final ArrayList<Integer> colorList = new ArrayList<>();
       colorList.add(Color.rgb(192, 255, 140));
       colorList.add(Color.rgb(255, 140, 157));

       dataSet.setColors(colorList);
       data.setValueTextSize(20f);
       data.setValueTextColor(Color.DKGRAY);

       pieChart.animateXY(1400, 1400);
    }
}
