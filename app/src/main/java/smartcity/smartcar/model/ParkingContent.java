package smartcity.smartcar.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe che rappresenta i parcheggi.
 * Contiene un valore statico ITEMS, ovvero una lista di tutti i parcheggi effettauti
 */
public class ParkingContent {
    public static final List<ParkingItem> ITEMS = new ArrayList<>();

    public static void addItem(ParkingContent.ParkingItem item) {
        ITEMS.add(item);
    }

    public static class ParkingItem {
        private final String id;
        private final double lat;
        private final double lon;
        private final boolean closed;
        private final String date;
        private final String time;

        public ParkingItem(String id, double lat, double lon, String closed, String datetime) {
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