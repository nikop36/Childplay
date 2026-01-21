package si.um.feri.rri.old;

import com.badlogic.gdx.utils.Array;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.UIComponent;
import si.um.feri.rri.services.WMSDataFetcher;

public class DataSystem {

    private final MarkerComponent markers;
    private final UIComponent ui;
    private final WMSDataFetcher fetcher = new WMSDataFetcher();

    private int loaded = 0;

    public DataSystem(MarkerComponent markers, UIComponent ui) {
        this.markers = markers;
        this.ui = ui;
    }

    public void loadAll(Runnable onComplete) {

        fetcher.fetchKindergartens(new WMSDataFetcher.DataCallback() {
            @Override public void onSuccess(Array<WMSDataFetcher.LocationData> data) {
                markers.markers.addAll(data);
                ui.kindergartens = data.size;
                check(onComplete);
            }
            @Override public void onFailure(String error) { check(onComplete); }
        });

        fetcher.fetchPlaygrounds(new WMSDataFetcher.DataCallback() {
            @Override public void onSuccess(Array<WMSDataFetcher.LocationData> data) {
                markers.markers.addAll(data);
                ui.playgrounds = data.size;
                check(onComplete);
            }
            @Override public void onFailure(String error) { check(onComplete); }
        });

        fetcher.fetchTrainRoute(new WMSDataFetcher.DataCallback() {
            @Override public void onSuccess(Array<WMSDataFetcher.LocationData> data) {
                markers.markers.addAll(data);
                ui.trainStops = data.size;
                check(onComplete);
            }
            @Override public void onFailure(String error) { check(onComplete); }
        });
    }

    private synchronized void check(Runnable onComplete) {
        loaded++;
        if (loaded >= 3) onComplete.run();
    }
}
