package si.um.feri.rri;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import si.um.feri.rri.component.*;
import si.um.feri.rri.manager.UIManager;
import si.um.feri.rri.system.*;

public class GameClass extends ApplicationAdapter {

    private SpriteBatch batch;

    private MapComponent map;
    private TileComponent tiles;
    private MarkerComponent markers;
    private UIComponent ui;

    private Stage stage;
    private Skin skin;

    private TileLoaderSystem tileLoader;
    private MarkerSystem markerSystem;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private InputSystem inputSystem;
    private DataSystem dataSystem;
    private MarkerEditSystem markerEditSystem;
    private TrainAnimationSystem trainAnimationSystem;
    private UIManager uiManager;


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

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, inputSystem));

        skin = new Skin(Gdx.files.internal("uiskin.json"));


        // Calculate how many tiles are needed to fill the viewport
        int tilesX = (int) Math.ceil(Gdx.graphics.getWidth() / (float) map.tileSize) + 1;
        int tilesY = (int) Math.ceil(Gdx.graphics.getHeight() / (float) map.tileSize) + 1;

        map.gridSize = Math.max(tilesX, tilesY);

        int maxTileY = (1 << map.zoom);
        int half = map.gridSize / 2;

        // World bounds = exactly the loaded tiles
        // Tiles loaded: centerTile - half + 0, centerTile - half + 1, ..., centerTile - half + (gridSize-1)
        int minTileX = map.centerTileX - half;
        int maxTileX = map.centerTileX - half + map.gridSize - 1;
        int minTileY = map.centerTileY - half;
        int maxTileY_tile = map.centerTileY - half + map.gridSize - 1;

        map.worldWidth = map.gridSize * map.tileSize;
        map.worldHeight = map.gridSize * map.tileSize;
        map.worldMinX = minTileX * map.tileSize;
        map.worldMinY = (maxTileY - maxTileY_tile - 1) * map.tileSize;

        Gdx.app.log("GameClass", "Grid size: " + map.gridSize + "x" + map.gridSize);
        Gdx.app.log("GameClass", "World bounds: [" + map.worldMinX + ", " + map.worldMinY +
                    "] to [" + (map.worldMinX + map.worldWidth) + ", " + (map.worldMinY + map.worldHeight) + "]");

        // Center camera in the middle of the loaded tile area
        float centerWorldX = map.worldMinX + map.worldWidth / 2f;
        float centerWorldY = map.worldMinY + map.worldHeight / 2f;
        map.camera.position.set(centerWorldX, centerWorldY, 0);
        Gdx.app.log("GameClass", "Camera centered at: [" + centerWorldX + ", " + centerWorldY + "]");
        map.camera.update();

        tileLoader = new TileLoaderSystem(map, tiles);
        markerSystem = new MarkerSystem(markers, map);
        dataSystem = new DataSystem(markers, ui);
        markerEditSystem = new MarkerEditSystem(markers, ui, markerSystem, dataSystem);
        renderSystem = new RenderSystem(map, tiles, markers, ui);
        cameraSystem = new CameraSystem(map);
        inputSystem = new InputSystem(cameraSystem, map, markers, ui, stage);
        trainAnimationSystem = new TrainAnimationSystem(markers);
        uiManager = new UIManager(stage, skin, ui, markers);

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, inputSystem));

        tileLoader.loadAllTiles();

        dataSystem.loadCustomMarkers();
        dataSystem.loadAll(() -> markerSystem.convertMarkersToWorld());
    }

    @Override
    public void render() {
        com.badlogic.gdx.graphics.GL20 gl = Gdx.gl;
        gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        inputSystem.update();
        trainAnimationSystem.update(Gdx.graphics.getDeltaTime());
        markerEditSystem.update();
        map.camera.update();

        // WORLD
        batch.begin();
        renderSystem.renderWorld(batch);
        batch.end();

        uiManager.update();

        // UI
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }
}
