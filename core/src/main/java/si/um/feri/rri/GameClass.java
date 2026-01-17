package si.um.feri.rri;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import si.um.feri.rri.component.*;
import si.um.feri.rri.system.*;

public class GameClass extends ApplicationAdapter {

    private SpriteBatch batch;

    private MapComponent map;
    private TileComponent tiles;
    private MarkerComponent markers;
    private UIComponent ui;

    private TileLoaderSystem tileLoader;
    private MarkerSystem markerSystem;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private InputSystem inputSystem;
    private DataSystem dataSystem;

    @Override
    public void create() {
        batch = new SpriteBatch();

        map = new MapComponent();
        tiles = new TileComponent();
        markers = new MarkerComponent();
        ui = new UIComponent();

        map.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        map.zoom = 13;
        double centerLat = 46.5547;
        double centerLon = 15.6459;

        map.centerTileX = (int) Math.floor((centerLon + 180.0) / 360.0 * (1 << map.zoom));
        map.centerTileY = (int) Math.floor(
            (1.0 - Math.log(Math.tan(Math.toRadians(centerLat)) +
                1.0 / Math.cos(Math.toRadians(centerLat))) / Math.PI) / 2.0 * (1 << map.zoom)
        );

        float centerWorldX = map.centerTileX * map.tileSize;
        float centerWorldY = map.centerTileY * map.tileSize;
        map.camera.position.set(centerWorldX, centerWorldY, 0);
        map.camera.update();

        tileLoader = new TileLoaderSystem(map, tiles);
        markerSystem = new MarkerSystem(markers, map);
        renderSystem = new RenderSystem(map, tiles, markers, ui);
        cameraSystem = new CameraSystem(map);
        inputSystem = new InputSystem(cameraSystem);
        dataSystem = new DataSystem(markers, ui);

        Gdx.input.setInputProcessor(inputSystem);

        tileLoader.loadAllTiles();

        dataSystem.loadAll(() -> markerSystem.convertMarkersToWorld());
    }

    @Override
    public void render() {
        map.camera.update();
        batch.begin();
        renderSystem.render(batch);
        batch.end();
    }
}
