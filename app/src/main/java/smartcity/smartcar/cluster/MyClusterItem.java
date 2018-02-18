package smartcity.smartcar.cluster;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Cluster Item che rappresenta un parcheggio effettuato dall'utente
 */
public class MyClusterItem implements ClusterItem {
    private LatLng position;
    private String title;
    private String snippet;

    public MyClusterItem(double lat, double lng, String title, String snippet) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.snippet = snippet;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }
}
