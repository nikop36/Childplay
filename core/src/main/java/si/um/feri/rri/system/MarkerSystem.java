package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
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
        markers.customMarkerPositions.clear();
        markers.trainRouteWorldCoords.clear();
        markers.route1SeparatorIndex = -1;

        int maxTileY = (1 << map.zoom);

        // --- 1) API MARKERS ---
        WMSDataFetcher.LocationData trainRoute1 = null;
        WMSDataFetcher.LocationData trainRoute2 = null;
        int maxRoutePoints1 = 0;
        int maxRoutePoints2 = 0;

        for (WMSDataFetcher.LocationData m : markers.markers) {

            double tileX = (m.longitude + 180.0) / 360.0 * (1 << map.zoom);
            double tileY = (1.0 - Math.log(Math.tan(Math.toRadians(m.latitude)) +
                1.0 / Math.cos(Math.toRadians(m.latitude))) / Math.PI) / 2.0 * (1 << map.zoom);

            float worldX = (float) (tileX * map.tileSize);
            float worldY = (float) ((maxTileY - tileY) * map.tileSize);

            markers.markerPositions.add(new Vector2(worldX, worldY));

            // Train route detection
            if ("train".equalsIgnoreCase(m.type) && m.routePoints != null && m.routePoints.size > 0) {
                if (m.routePoints.size > maxRoutePoints1) {
                    trainRoute2 = trainRoute1;
                    maxRoutePoints2 = maxRoutePoints1;

                    trainRoute1 = m;
                    maxRoutePoints1 = m.routePoints.size;
                } else if (m.routePoints.size > maxRoutePoints2) {
                    trainRoute2 = m;
                    maxRoutePoints2 = m.routePoints.size;
                }
            }

            // Log first 3 API markers
            if (markers.markerPositions.size <= 3) {
                Gdx.app.log("MarkerSystem", "[API] " + m.name +
                    " -> world[" + worldX + "," + worldY + "]");
            }
        }

        // --- 2) CUSTOM MARKERS ---
        for (WMSDataFetcher.LocationData m : markers.customMarkers) {

            double tileX = (m.longitude + 180.0) / 360.0 * (1 << map.zoom);
            double tileY = (1.0 - Math.log(Math.tan(Math.toRadians(m.latitude)) +
                1.0 / Math.cos(Math.toRadians(m.latitude))) / Math.PI) / 2.0 * (1 << map.zoom);

            float worldX = (float) (tileX * map.tileSize);
            float worldY = (float) ((maxTileY - tileY) * map.tileSize);

            markers.customMarkerPositions.add(new Vector2(worldX, worldY));

            // Log first 3 custom markers
            if (markers.customMarkerPositions.size <= 3) {
                Gdx.app.log("MarkerSystem", "[CUSTOM] " + m.name +
                    " -> world[" + worldX + "," + worldY + "]");
            }
        }

        // --- 3) TRAIN ROUTES ---
        if (trainRoute1 != null && trainRoute1.routePoints != null) {
            for (Vector2 p : trainRoute1.routePoints) {
                double tx = (p.x + 180.0) / 360.0 * (1 << map.zoom);
                double ty = (1.0 - Math.log(Math.tan(Math.toRadians(p.y)) +
                    1.0 / Math.cos(Math.toRadians(p.y))) / Math.PI) / 2.0 * (1 << map.zoom);

                markers.trainRouteWorldCoords.add(new Vector2(
                    (float) (tx * map.tileSize),
                    (float) ((maxTileY - ty) * map.tileSize)
                ));
            }
        }

        if (trainRoute2 != null && trainRoute2.routePoints != null) {
            markers.route1SeparatorIndex = markers.trainRouteWorldCoords.size;
            markers.trainRouteWorldCoords.add(new Vector2(-999999, -999999));

            for (Vector2 p : trainRoute2.routePoints) {
                double tx = (p.x + 180.0) / 360.0 * (1 << map.zoom);
                double ty = (1.0 - Math.log(Math.tan(Math.toRadians(p.y)) +
                    1.0 / Math.cos(Math.toRadians(p.y))) / Math.PI) / 2.0 * (1 << map.zoom);

                markers.trainRouteWorldCoords.add(new Vector2(
                    (float) (tx * map.tileSize),
                    (float) ((maxTileY - ty) * map.tileSize)
                ));
            }
        }
    }
}

