package si.um.feri.rri.component;

import si.um.feri.rri.component.enums.MarkerType;

import java.util.EnumSet;

public class UIComponent {

    // Current Data
    public int kindergartens;
    public int playgrounds;
    public int trainStops;

    // Filters (privzeto: vse vključeno)
    public EnumSet<MarkerType> activeFilters = EnumSet.allOf(MarkerType.class);

    // Edit mode toggle
    public boolean editMode = false;

    // UI windows
    public boolean showAddWindow = false;
    public boolean showEditWindow = false;

    // For adding/editing markers
    public double newMarkerLat;
    public double newMarkerLon;
    public String newMarkerName = "";
    public String newMarkerType = ""; // npr. "kindergarten", "playground", "train", "custom"
    public String newMarkerDescription = "";

    // signals
    public boolean addMarkerRequested = false;
    public boolean saveEditRequested = false;
    public boolean deleteMarkerRequested = false;

    // For editing markers
    public si.um.feri.rri.services.WMSDataFetcher.LocationData editingMarker = null;
}


