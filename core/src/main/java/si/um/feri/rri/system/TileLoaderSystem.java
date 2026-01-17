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
                if (data == null) {
                    retry();
                    return;
                }

                Pixmap pix = new Pixmap(data, 0, data.length);

                Gdx.app.postRunnable(() -> {
                    Texture tex = new Texture(pix);
                    pix.dispose();

                    TileComponent.Tile t = new TileComponent.Tile();
                    t.tileX = tileX;
                    t.tileY = tileY;
                    t.worldX = tileX * map.tileSize;
                    t.worldY = tileY * map.tileSize;
                    t.texture = tex;

                    tiles.tiles[gx][gy] = t;

                    tiles.loadedTiles++;
                    if (tiles.loadedTiles == tiles.totalTiles) {
                        tiles.loaded = true;
                    }
                });
            }

            @Override public void failed(Throwable t) { retry(); }
            @Override public void cancelled() { retry(); }

            private void retry() {
                if (retries > 0) {
                    requestTile(url, gx, gy, tileX, tileY, retries - 1);
                } else {
                    Gdx.app.postRunnable(() -> {
                        tiles.loadedTiles++;
                        if (tiles.loadedTiles == tiles.totalTiles) {
                            tiles.loaded = true;
                        }
                    });
                }
            }
        });
    }

    /*
    public void loadAllTiles() {

        tiles.tiles = new TileComponent.Tile[map.gridSize][map.gridSize];

        tiles.totalTiles = map.gridSize * map.gridSize;
        tiles.loadedTiles = 0;
        tiles.loaded = false;

        int half = map.gridSize / 2;

        for (int gy = 0; gy < map.gridSize; gy++) {
            for (int gx = 0; gx < map.gridSize; gx++) {

                int offsetX = gx - half;
                int offsetY = gy - half;

                int tileX = map.centerTileX + offsetX;
                int tileY = map.centerTileY + offsetY;

                String url = "https://maps.geoapify.com/v1/tile/osm-bright/" +
                        map.zoom + "/" + tileX + "/" + tileY + ".png?apiKey=" + API_KEY;

                requestTile(url, gx, gy, tileX, tileY, 3);
            }
        }
    }

    */

    public void loadAllTiles() {

        int half = map.gridSize / 2;
        tiles.tiles = new TileComponent.Tile[map.gridSize][map.gridSize];

        for (int gy = -half; gy <= half; gy++) {
            for (int gx = -half; gx <= half; gx++) {

                int tileX = map.centerTileX + gx;
                int tileY = map.centerTileY + gy;

                String url = "https://maps.geoapify.com/v1/tile/osm-bright/" +
                    map.zoom + "/" + tileX + "/" + tileY + ".png?apiKey=" + API_KEY;

                final int fx = gx + half;
                final int fy = gy + half;

                Net.HttpRequest req = new HttpRequestBuilder()
                    .newRequest()
                    .method(Net.HttpMethods.GET)
                    .url(url)
                    .build();

                Gdx.net.sendHttpRequest(req, new Net.HttpResponseListener() {
                    @Override
                    public void handleHttpResponse(Net.HttpResponse httpResponse) {
                        byte[] data = httpResponse.getResult();
                        if (data == null) return;

                        Pixmap pix = new Pixmap(data, 0, data.length);

                        Gdx.app.postRunnable(() -> {
                            Texture tex = new Texture(pix);
                            pix.dispose();

                            TileComponent.Tile t = new TileComponent.Tile();
                            t.tileX = tileX;
                            t.tileY = tileY;
                            t.worldX = tileX * map.tileSize;
                            t.worldY = tileY * map.tileSize;
                            t.texture = tex;

                            tiles.tiles[fx][fy] = t;
                        });
                    }

                    @Override public void failed(Throwable t) {}
                    @Override public void cancelled() {}
                });
            }
        }

        tiles.loaded = true;

        // world size (rough bounds) based on tile indices
        map.worldWidth = (map.centerTileX + half + 1) * map.tileSize;
        map.worldHeight = (map.centerTileY + half + 1) * map.tileSize;
    }
}
