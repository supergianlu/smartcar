package smartcity.smartcar.model;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smartcity.smartcar.R;
import smartcity.smartcar.UrlConnectionAsyncTask;

public class ParkingContent implements UrlConnectionAsyncTask.UrlConnectionListener {
    public static final List<ParkingItem> ITEMS = new ArrayList<>();
    public static final Map<String, ParkingItem> ITEM_MAP = new HashMap<>();

    public ParkingContent(Context context, String user){
        try {
            Bundle data = new Bundle();
            data.putString("username", user);
            System.out.println(user);
            new UrlConnectionAsyncTask(new URL(context.getString(R.string.get_parking)), this, context).execute(data);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static void addItem(ParkingContent.ParkingItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }

    @Override
    public void handleResponse(JSONObject response, Bundle extra) {
        if(response.length() != 0) {
            try {
                final int code = response.getInt("code");
                if(code == 2) {
                    final JSONArray parkingArray = response.getJSONObject("extra").getJSONArray("data");
                    for(int i = 0; i < parkingArray.length(); i++) {
                        JSONObject parking = parkingArray.getJSONObject(i);
                        addItem(new ParkingItem(parking.getString("id"), parking.getDouble("lat"), parking.getDouble("lon"), parking.getString("closed"), parking.getString("date")));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ParkingItem {
        private final String id;
        private final double lat;
        private final double lon;
        private final boolean closed;
        private final String date;
        private final String time;

        ParkingItem(String id, double lat, double lon, String closed, String datetime) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.closed = Integer.parseInt(closed) != 0;;
            this.date = datetime.substring(0, 10);
            this.time = datetime.substring(11, 16);
        }

        public String getId() {
            return id;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public boolean isClosed() {
            return closed;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }
    }
}