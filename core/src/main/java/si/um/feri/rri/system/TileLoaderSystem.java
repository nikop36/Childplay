package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.TileComponent;

public class TileLoaderSystem {

    private final MapComponent map;
    private final TileComponent tiles;

    private static final String API_KEY = "7a9eb4ca443b47ec8a7e9db8bfb0cbd5";
    private static final String CACHE_DIR = "tiles_cache/";

    public TileLoaderSystem(MapComponent map, TileComponent tiles) {
        this.map = map;
        this.tiles = tiles;
        ensureCacheDirectory();
    }

    private void ensureCacheDirectory() {
        FileHandle cacheDir = Gdx.files.local(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            Gdx.app.log("TileLoader", "Created cache directory: " + CACHE_DIR);
        }
    }

    private String getTileCachePath(int zoom, int tileX, int tileY) {
        return CACHE_DIR + zoom + "_" + tileX + "_" + tileY + ".png";
    }

    private void loadTile(int gx, int gy, int tileX, int tileY) {
        String cachePath = getTileCachePath(map.zoom, tileX, tileY);
        FileHandle cacheFile = Gdx.files.local(cachePath);

        if (cacheFile.exists()) {
            loadTileFromCache(cacheFile, gx, gy, tileX, tileY);
        } else {
            String url = "https://maps.geoapify.com/v1/tile/osm-bright/" +
                map.zoom + "/" + tileX + "/" + tileY + ".png?apiKey=" + API_KEY;
            requestTile(url, gx, gy, tileX, tileY, cachePath, 3);
        }
    }

    private void loadTileFromCache(FileHandle cacheFile, int gx, int gy, int tileX, int tileY) {
        try {
            Pixmap pix = new Pixmap(cacheFile);
            Texture tex = new Texture(pix);
            pix.dispose();

            int maxTileY = (1 << map.zoom);
            TileComponent.Tile t = new TileComponent.Tile();
            t.tileX = tileX;
            t.tileY = tileY;
            t.worldX = tileX * map.tileSize;
            t.worldY = (maxTileY - tileY - 1) * map.tileSize;
            t.texture = tex;

            tiles.tiles[gx][gy] = t;

            tiles.loadedTiles++;
            if (tiles.loadedTiles == tiles.totalTiles) {
                tiles.loaded = true;
                Gdx.app.log("TileLoader", "All tiles loaded!");
            }
        } catch (Exception e) {
            Gdx.app.error("TileLoader", "Failed to load cached tile [" + tileX + "," + tileY + "]: " + e.getMessage());
            cacheFile.delete();
            loadTile(gx, gy, tileX, tileY);
        }
    }

    private void requestTile(String url, int gx, int gy, int tileX, int tileY, String cachePath, int retries) {

        Net.HttpRequest req = new HttpRequestBuilder()
            .newRequest()
            .method(Net.HttpMethods.GET)
            .url(url)
            .build();

        Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                byte[] data = httpResponse.getResult();
                if (data == null || data.length == 0) {
                    retry();
                    return;
                }

                Gdx.app.postRunnable(() -> {
                    try {
                        Gdx.files.local(cachePath).writeBytes(data, false);

                        Pixmap pix = new Pixmap(data, 0, data.length);
                        Texture tex = new Texture(pix);
                        pix.dispose();

                        int maxTileY = (1 << map.zoom);
                        TileComponent.Tile t = new TileComponent.Tile();
                        t.tileX = tileX;
                        t.tileY = tileY;
                        t.worldX = tileX * map.tileSize;
                        t.worldY = (maxTileY - tileY - 1) * map.tileSize;
                        t.texture = tex;

                        tiles.tiles[gx][gy] = t;

                        tiles.loadedTiles++;
                        if (tiles.loadedTiles == tiles.totalTiles) {
                            tiles.loaded = true;
                            Gdx.app.log("TileLoader", "All tiles loaded!");
                        }
                    } catch (Exception e) {
                        Gdx.app.error("TileLoader", "Failed to create texture for tile [" + tileX + "," + tileY + "]: " + e.getMessage());
                        retry();
                    }
                });
            }

            @Override public void failed(Throwable t) {
                Gdx.app.error("TileLoader", "HTTP failed for tile [" + tileX + "," + tileY + "]: " + t.getMessage());
                retry();
            }

            @Override public void cancelled() {
                retry();
            }

            private void retry() {
                if (retries > 0) {
                    Gdx.app.log("TileLoader", "Retrying tile [" + tileX + "," + tileY + "], attempts left: " + retries);
                    requestTile(url, gx, gy, tileX, tileY, cachePath, retries - 1);
                } else {
                    Gdx.app.error("TileLoader", "Failed to load tile [" + tileX + "," + tileY + "] after all retries");
                    Gdx.app.postRunnable(() -> {
                        tiles.loadedTiles++;
                        if (tiles.loadedTiles == tiles.totalTiles) {
                            tiles.loaded = true;
                            Gdx.app.log("TileLoader", "All tiles loaded (with some failures)!");
                        }
                    });
                }
            }
        });
    }

    public void loadAllTiles() {

        int half = map.gridSize / 2;
        tiles.tiles = new TileComponent.Tile[map.gridSize][map.gridSize];

        tiles.totalTiles = map.gridSize * map.gridSize;
        tiles.loadedTiles = 0;
        tiles.loaded = false;

        Gdx.app.log("TileLoader", "Loading " + tiles.totalTiles + " tiles (" + map.gridSize + "x" + map.gridSize + " grid)");
        Gdx.app.log("TileLoader", "Center tile: [" + map.centerTileX + ", " + map.centerTileY + "]");

        for (int gy = 0; gy < map.gridSize; gy++) {
            for (int gx = 0; gx < map.gridSize; gx++) {

                int tileX = map.centerTileX - half + gx;
                int tileY = map.centerTileY - half + gy;

                loadTile(gx, gy, tileX, tileY);
            }
        }
    }
}
