package si.um.feri.rri.system;

import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.UIComponent;
import si.um.feri.rri.services.WMSDataFetcher;

public class MarkerEditSystem {

    private final MarkerComponent markers;
    private final UIComponent ui;
    private final MarkerSystem markerSystem;
    private final DataSystem dataSystem;

    public MarkerEditSystem(MarkerComponent markers, UIComponent ui,
                            MarkerSystem markerSystem, DataSystem dataSystem) {
        this.markers = markers;
        this.ui = ui;
        this.markerSystem = markerSystem;
        this.dataSystem = dataSystem;
    }

    public void update() {

        // ADD MARKER
        if (ui.addMarkerRequested) {
            addMarker();
            ui.addMarkerRequested = false;
        }

        // SAVE EDITED MARKER
        if (ui.saveEditRequested) {
            saveEditedMarker();
            ui.saveEditRequested = false;
        }

        // DELETE MARKER
        if (ui.deleteMarkerRequested) {
            deleteMarker();
            ui.deleteMarkerRequested = false;
        }
    }

    private void addMarker() {
        WMSDataFetcher.LocationData m = new WMSDataFetcher.LocationData();
        m.name = ui.newMarkerName;
        m.type = ui.newMarkerType;
        m.latitude = ui.newMarkerLat;
        m.longitude = ui.newMarkerLon;

        markers.customMarkers.add(m);

        markerSystem.convertMarkersToWorld();
        dataSystem.saveCustomMarkers();
    }

    private void saveEditedMarker() {
        WMSDataFetcher.LocationData m = ui.editingMarker;
        if (m == null) return;

        m.name = ui.newMarkerName;
        m.type = ui.newMarkerType;
        m.latitude = ui.newMarkerLat;
        m.longitude = ui.newMarkerLon;

        markerSystem.convertMarkersToWorld();
        dataSystem.saveCustomMarkers();
    }

    private void deleteMarker() {
        if (ui.editingMarker == null) return;

        markers.customMarkers.removeValue(ui.editingMarker, true);
        markerSystem.convertMarkersToWorld();
        dataSystem.saveCustomMarkers();
    }
}

