package si.um.feri.rri.system;

import com.badlogic.gdx.math.Vector2;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.services.WMSDataFetcher;

public class MarkerSystem {

    private final MarkerComponent markers;
    private final MapComponent map;

    public MarkerSystem(MarkerComponent markers, MapComponent map) {
        this.markers = markers;
        this.map = map;
    }

    public void convertMarkersToWorld() {

        markers.markerPositions.clear();

        for (WMSDataFetcher.LocationData m : markers.markers) {

            double tileX = (m.longitude + 180.0) / 360.0 * (1 << map.zoom);
            double tileY = (1.0 - Math.log(Math.tan(Math.toRadians(m.latitude)) +
                1.0 / Math.cos(Math.toRadians(m.latitude))) / Math.PI) / 2.0 * (1 << map.zoom);

            float worldX = (float) tileX * map.tileSize;
            float worldY = (float) tileY * map.tileSize;

            markers.markerPositions.add(new Vector2(worldX, worldY));
        }
    }
}
