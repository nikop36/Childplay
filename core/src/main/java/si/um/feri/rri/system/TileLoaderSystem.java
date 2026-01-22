package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpRequestBuilder;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.TileComponent;

public class TileLoaderSystem {

    private final MapComponent map;
    private final TileComponent tiles;

    private static final String API_KEY = "7a9eb4ca443b47ec8a7e9db8bfb0cbd5";

    public TileLoaderSystem(MapComponent map, TileComponent tiles) {
        this.map = map;
        this.tiles = tiles;
    }

    private void requestTile(String url, int gx, int gy, int tileX, int tileY, int retries) {

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

                        // Log bottom-right tile details
                        if (gx == map.gridSize - 1 && gy == map.gridSize - 1) {
                            Gdx.app.log("TileLoader", "Bottom-Right tile loaded: tile[" + tileX + "," + tileY +
                                       "] at world[" + t.worldX + "," + t.worldY + "] in grid[" + gx + "," + gy + "]");
                        }

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
                    requestTile(url, gx, gy, tileX, tileY, retries - 1);
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

                // Log corner tiles
                if ((gx == 0 || gx == map.gridSize - 1) && (gy == 0 || gy == map.gridSize - 1)) {
                    String corner = (gx == 0 ? "Left" : "Right") + "-" + (gy == 0 ? "Top" : "Bottom");
                    Gdx.app.log("TileLoader", corner + " corner: grid[" + gx + "," + gy + "] = tile[" + tileX + "," + tileY + "]");
                }

                String url = "https://maps.geoapify.com/v1/tile/osm-bright/" +
                    map.zoom + "/" + tileX + "/" + tileY + ".png?apiKey=" + API_KEY;

                requestTile(url, gx, gy, tileX, tileY, 3);
            }
        }
    }
}
