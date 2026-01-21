package si.um.feri.rri.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import si.um.feri.rri.services.WMSDataFetcher;

public class MarkerComponent {

    // Fetched markers
    public Array<WMSDataFetcher.LocationData> markers = new Array<>();
    public Array<Vector2> markerPositions = new Array<>();

    // Custom markers
    public Array<WMSDataFetcher.LocationData> customMarkers = new Array<>();
    public Array<Vector2> customMarkerPositions = new Array<>();

    // Selected and hover mode
    public WMSDataFetcher.LocationData selected = null;
    public WMSDataFetcher.LocationData hovered = null;

    // Animation state for trains
    public Array<Vector2> trainRouteWorldCoords = new Array<>();

    public int route1SeparatorIndex = -1;
    public float train1Progress = 0f;
    public boolean train1Forward = true;
    public float train2Progress = 0f;
    public boolean train2Forward = true;
}
