package si. um.feri.rri;

import com.badlogic. gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com. badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class GameClass extends ApplicationAdapter {
    private SpriteBatch batch;
    private MapRenderer mapRenderer;
    private WMSDataFetcher dataFetcher;
    private boolean dataLoaded = false;
    private int dataLoadedCount = 0;

    // UI elements
    private BitmapFont font;
    private BitmapFont titleFont;
    private ShapeRenderer shapeRenderer;
    private Array<WMSDataFetcher.LocationData> allMarkers;

    private int kindergartens = 0;
    private int playgrounds = 0;
    private int trainStops = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        mapRenderer = new MapRenderer();
        dataFetcher = new WMSDataFetcher();

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);

        titleFont = new BitmapFont();
        titleFont.setColor(Color. YELLOW);
        titleFont.getData().setScale(2f);

        shapeRenderer = new ShapeRenderer();
        allMarkers = new Array<>();

        // Fetch all data from Maribor
        loadAllData();
    }

    private void loadAllData() {
        // Fetch kindergartens
        dataFetcher.fetchKindergartens(new WMSDataFetcher.DataCallback() {
            @Override
            public void onSuccess(Array<WMSDataFetcher.LocationData> data) {
                Gdx.app.log("GameClass", "Loaded " + data.size + " kindergartens");
                mapRenderer.addMarkers(data);
                allMarkers.addAll(data);
                kindergartens = data.size;
                checkAndLoadMap();
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.error("GameClass", "Failed to load kindergartens: " + error);
                checkAndLoadMap();
            }
        });

        // Fetch playgrounds
        dataFetcher.fetchPlaygrounds(new WMSDataFetcher.DataCallback() {
            @Override
            public void onSuccess(Array<WMSDataFetcher. LocationData> data) {
                Gdx.app.log("GameClass", "Loaded " + data.size + " playgrounds");
                mapRenderer.addMarkers(data);
                allMarkers.addAll(data);
                playgrounds = data.size;
                checkAndLoadMap();
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.error("GameClass", "Failed to load playgrounds: " + error);
                checkAndLoadMap();
            }
        });

        // Fetch train route
        dataFetcher.fetchTrainRoute(new WMSDataFetcher.DataCallback() {
            @Override
            public void onSuccess(Array<WMSDataFetcher.LocationData> data) {
                Gdx.app.log("GameClass", "Loaded train route with " + data.size + " points");
                mapRenderer.addMarkers(data);
                allMarkers.addAll(data);
                trainStops = data.size;
                checkAndLoadMap();
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.error("GameClass", "Failed to load train route: " + error);
                checkAndLoadMap();
            }
        });
    }

    private synchronized void checkAndLoadMap() {
        dataLoadedCount++;
        if (dataLoadedCount >= 3 && !dataLoaded) {
            dataLoaded = true;
            Gdx.app.postRunnable(() -> mapRenderer.loadMap());
        }
    }

    @Override
    public void render() {
        ScreenUtils. clear(0.1f, 0.1f, 0.15f, 1f);

        // Draw map
        batch.begin();
        mapRenderer.render(batch, 0, 0);

        // Draw UI overlay
        drawUI();

        batch.end();
    }

    private void drawUI() {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics. getHeight();

        batch.end();

        // Draw semi-transparent panels
        Gdx.gl. glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Top panel
        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.85f);
        shapeRenderer. rect(20, screenHeight - 100, 400, 80);

        // Legend panel
        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.85f);
        shapeRenderer.rect(20, 20, 320, 140);

        // Draw legend color boxes
        shapeRenderer.setColor(0, 0, 1, 0.8f); // Blue
        shapeRenderer. circle(40, 130, 8);

        shapeRenderer.setColor(0, 1, 0, 0.8f); // Green
        shapeRenderer.circle(40, 95, 8);

        shapeRenderer.setColor(1, 0, 0, 0.8f); // Red
        shapeRenderer.circle(40, 60, 8);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        // Title
        float y = screenHeight - 30;
        titleFont.draw(batch, "Childplay Maribor", 30, y);
        y -= 35;
        font.setColor(Color. LIGHT_GRAY);
        font.draw(batch, "Interactive Map with Real Data", 30, y);

        // Legend
        font.setColor(Color.CYAN);
        font.draw(batch, "Kindergartens: " + kindergartens, 60, 135);

        font.setColor(Color. GREEN);
        font.draw(batch, "Playgrounds: " + playgrounds, 60, 100);

        font.setColor(Color. ORANGE);
        font.draw(batch, "Train Route: " + trainStops, 60, 65);

        font.setColor(Color.WHITE);
        font.draw(batch, "Total:  " + allMarkers.size + " locations", 30, 30);

        // Loading indicator
        if (! mapRenderer.isMapLoaded()) {
            font.setColor(Color. YELLOW);
            font.draw(batch, "Loading map tiles...", screenWidth / 2 - 100, screenHeight / 2);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        mapRenderer.dispose();
        font.dispose();
        titleFont.dispose();
        shapeRenderer.dispose();
    }
}
