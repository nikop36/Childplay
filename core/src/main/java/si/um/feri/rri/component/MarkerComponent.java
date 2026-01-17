package si.um.feri.rri.component;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import si.um.feri.rri.services.WMSDataFetcher;

public class MarkerComponent {
    public Array<WMSDataFetcher.LocationData> markers = new Array<>();
    public Array<Vector2> markerPositions = new Array<>();
    public WMSDataFetcher.LocationData selected = null;
}
