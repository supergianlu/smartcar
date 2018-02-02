package smartcity.smartcar.cluster;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Window;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.maps.android.clustering.ClusterItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import smartcity.smartcar.R;


public class ParkingDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog_parking);
        TextView title = findViewById(R.id.title);
        TextView description = findViewById(R.id.description);
        description.setMovementMethod(new ScrollingMovementMethod());
        StringBuilder stringBuilder = new StringBuilder();
        String clusterCollection = getIntent().getStringExtra("clusterCollection");
        final JSONArray json;
        try {
            json = new JSONArray(clusterCollection);
            json.length();
            final int n = json.length();
            title.setText("Hai parcheggiato "+n+" volte in questo posto:");
            for (int i = 0; i < n; ++i) {
                final JSONObject carMarker = json.getJSONObject(i);
                stringBuilder.append("Parcheggio " + (i + 1) + "\n");
                /*final JSONObject carPosition = carMarker.getJSONObject("position");
                stringBuilder.append("\""+carPosition.getString("latitude")+"\",\""+carPosition.getString("longitude")+"\"\n");*/
                stringBuilder.append(carMarker.getString("snippet") + "\n");
                stringBuilder.append(carMarker.getString("title") + "\n\n");
            }
            description.setText(stringBuilder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
