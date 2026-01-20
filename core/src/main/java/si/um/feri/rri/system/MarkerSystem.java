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
        markers.trainRouteWorldCoords.clear();
        
        int maxTileY = (1 << map.zoom);
        
        // Find the two train features with the most route points
        WMSDataFetcher.LocationData trainRoute1 = null;
        WMSDataFetcher.LocationData trainRoute2 = null;
        int maxRoutePoints1 = 0;
        int maxRoutePoints2 = 0;

        for (WMSDataFetcher.LocationData m : markers.markers) {

            double tileX = (m.longitude + 180.0) / 360.0 * (1 << map.zoom);
            double tileY = (1.0 - Math.log(Math.tan(Math.toRadians(m.latitude)) +
                1.0 / Math.cos(Math.toRadians(m.latitude))) / Math.PI) / 2.0 * (1 << map.zoom);

            // Convert to world coordinates with Y-axis inversion
            // NOTE: No -1 for continuous coordinates
            float worldX = (float) (tileX * map.tileSize);
            float worldY = (float) ((maxTileY - tileY) * map.tileSize);

            markers.markerPositions.add(new Vector2(worldX, worldY));
            
            // Find two train features with most route points
            if (m.type.equals("train") && m.routePoints.size > 0) {
                if (m.routePoints.size > maxRoutePoints1) {
                    // Shift down to second place
                    trainRoute2 = trainRoute1;
                    maxRoutePoints2 = maxRoutePoints1;
                    // New first place
                    trainRoute1 = m;
                    maxRoutePoints1 = m.routePoints.size;
                } else if (m.routePoints.size > maxRoutePoints2) {
                    trainRoute2 = m;
                    maxRoutePoints2 = m.routePoints.size;
                }
            }
            
            // Log first 3 markers
            if (markers.markerPositions.size <= 3) {
                com.badlogic.gdx.Gdx.app.log("MarkerSystem", m.name + " at lat=" + m.latitude + " lon=" + m.longitude +
                    " -> tile[" + String.format("%.2f", tileX) + "," + String.format("%.2f", tileY) + 
                    "] -> world[" + String.format("%.1f", worldX) + "," + String.format("%.1f", worldY) + "]" +
                    (m.routePoints.size > 0 ? " (route points: " + m.routePoints.size + ")" : ""));
            }
        }
        
        // Convert both train routes
        if (trainRoute1 != null && trainRoute1.routePoints.size > 0) {
            for (com.badlogic.gdx.math.Vector2 routePoint : trainRoute1.routePoints) {
                double rTileX = (routePoint.x + 180.0) / 360.0 * (1 << map.zoom);
                double rTileY = (1.0 - Math.log(Math.tan(Math.toRadians(routePoint.y)) +
                    1.0 / Math.cos(Math.toRadians(routePoint.y))) / Math.PI) / 2.0 * (1 << map.zoom);
                
                float rWorldX = (float) (rTileX * map.tileSize);
                float rWorldY = (float) ((maxTileY - rTileY) * map.tileSize);
                
                markers.trainRouteWorldCoords.add(new Vector2(rWorldX, rWorldY));
            }
            com.badlogic.gdx.Gdx.app.log("MarkerSystem", "Route 1: " + trainRoute1.routePoints.size + " points");
        }
        
        if (trainRoute2 != null && trainRoute2.routePoints.size > 0) {
            // Add a separator (null or very far point) to indicate new route segment
            markers.route1SeparatorIndex = markers.trainRouteWorldCoords.size;
            markers.trainRouteWorldCoords.add(new Vector2(-999999, -999999));
            
            for (com.badlogic.gdx.math.Vector2 routePoint : trainRoute2.routePoints) {
                double rTileX = (routePoint.x + 180.0) / 360.0 * (1 << map.zoom);
                double rTileY = (1.0 - Math.log(Math.tan(Math.toRadians(routePoint.y)) +
                    1.0 / Math.cos(Math.toRadians(routePoint.y))) / Math.PI) / 2.0 * (1 << map.zoom);
                
                float rWorldX = (float) (rTileX * map.tileSize);
                float rWorldY = (float) ((maxTileY - rTileY) * map.tileSize);
                
                markers.trainRouteWorldCoords.add(new Vector2(rWorldX, rWorldY));
            }
            com.badlogic.gdx.Gdx.app.log("MarkerSystem", "Route 2: " + trainRoute2.routePoints.size + " points");
        }
        
        com.badlogic.gdx.Gdx.app.log("MarkerSystem", "Converted " + markers.markerPositions.size + " markers, " + 
            markers.trainRouteWorldCoords.size + " train route points");
    }
}
