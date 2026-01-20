package si.um.feri.rri.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import si.um.feri.rri.services.WMSDataFetcher;

public class MarkerComponent {
    public Array<WMSDataFetcher.LocationData> markers = new Array<>();
    public Array<Vector2> markerPositions = new Array<>();
    public WMSDataFetcher.LocationData selected = null;
    public WMSDataFetcher.LocationData hovered = null;
    public Array<Vector2> trainRouteWorldCoords = new Array<>();
    
    // Animation state for trains
    public int route1SeparatorIndex = -1;
    public float train1Progress = 0f;
    public boolean train1Forward = true;
    public float train2Progress = 0f;
    public boolean train2Forward = true;
}
