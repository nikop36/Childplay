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
        
        int maxTileY = (1 << map.zoom);

        for (WMSDataFetcher.LocationData m : markers.markers) {

            double tileX = (m.longitude + 180.0) / 360.0 * (1 << map.zoom);
            double tileY = (1.0 - Math.log(Math.tan(Math.toRadians(m.latitude)) +
                1.0 / Math.cos(Math.toRadians(m.latitude))) / Math.PI) / 2.0 * (1 << map.zoom);

            // Convert to world coordinates with Y-axis inversion
            // NOTE: No -1 for continuous coordinates
            float worldX = (float) (tileX * map.tileSize);
            float worldY = (float) ((maxTileY - tileY) * map.tileSize);

            markers.markerPositions.add(new Vector2(worldX, worldY));
            
            // Log first 3 markers
            if (markers.markerPositions.size <= 3) {
                com.badlogic.gdx.Gdx.app.log("MarkerSystem", m.name + " at lat=" + m.latitude + " lon=" + m.longitude +
                    " -> tile[" + String.format("%.2f", tileX) + "," + String.format("%.2f", tileY) + 
                    "] -> world[" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + "]");
            }
        }
        
        com.badlogic.gdx.Gdx.app.log("MarkerSystem", "Converted " + markers.markerPositions.size + " markers");
    }
}
