package si.um.feri.rri.old;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import si.um.feri.rri.services.WMSDataFetcher;

public class MapRenderer {
    private static final String GEOAPIFY_API_KEY = "7a9eb4ca443b47ec8a7e9db8bfb0cbd5";

    private Texture[][] mapTiles;
    private boolean isLoading = false;
    private Array<WMSDataFetcher.LocationData> markers;
    private WMSDataFetcher.LocationData selectedMarker = null;
    private int tilesLoaded = 0;
    private int totalTilesToLoad = 0;

    // Maribor city center coordinates
    private double centerLat = 46.5547;
    private double centerLon = 15.6459;
    private int zoom = 13;
    private int tileSize = 256;

    // Tile grid (3x3)
    private int gridSize = 3;
    private int centerTileX;
    private int centerTileY;

    private ShapeRenderer shapeRenderer;
    private final ObjectMap<String, Texture> tileCache = new ObjectMap<>();

    // Panning offset (in pixels) - no interpolation
    private float panOffsetX = 0;
    private float panOffsetY = 0;

    public MapRenderer() {
        markers = new Array<>();
        mapTiles = new Texture[gridSize][gridSize];
        shapeRenderer = new ShapeRenderer();
    }

    public int getZoom() { return zoom; }

    public WMSDataFetcher.LocationData getSelectedMarker() {
        return selectedMarker;
    }

    public void pan(float dx, float dy) {
        // Direct panning without reloading
        panOffsetX += dx;
        panOffsetY -= dy;
    }

    public void finalizePan() {
        // Convert pixel offset to coordinate change
        double lonPerPixel = 360.0 / (256 * Math.pow(2, zoom));
        double latPerPixel = lonPerPixel;

        centerLon -= panOffsetX * lonPerPixel;
        centerLat += panOffsetY * latPerPixel;

        // Reset offset
        panOffsetX = 0;
        panOffsetY = 0;

        // Reload tiles with new center
        loadMap();
    }

    public void checkMarkerClick(float screenX, float screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        float fixedY = Gdx.graphics.getHeight() - screenY;

        float startX = (screenWidth - (gridSize * tileSize)) / 2f;
        float startY = (screenHeight - (gridSize * tileSize)) / 2f;

        for (WMSDataFetcher.LocationData marker : markers) {
            double markerTileX = (marker.longitude + 180.0) / 360.0 * (1 << zoom);
            double markerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(marker.latitude)) +
                1.0 / Math.cos(Math.toRadians(marker.latitude))) / Math.PI) / 2.0 * (1 << zoom);

            double pixelX = (markerTileX - (centerTileX - 1)) * tileSize;
            double pixelY = (markerTileY - (centerTileY - 1)) * tileSize;

            float mx = startX + (float) pixelX;
            float my = startY + (gridSize * tileSize) - (float) pixelY;

            float dist = Vector2.dst(screenX, fixedY, mx, my);

            if (dist < 10) {
                selectedMarker = marker;
                return;
            }
        }

        selectedMarker = null;
    }

    public void loadMap() {
        if (isLoading) return;
        isLoading = true;

        Gdx.app.log("MapRenderer", "Loading 3x3 Geoapify tile grid for Maribor");

        // Calculate center tile
        centerTileX = (int) Math.floor((centerLon + 180.0) / 360.0 * (1 << zoom));
        centerTileY = (int) Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(centerLat)) +
            1.0 / Math.cos(Math.toRadians(centerLat))) / Math.PI) / 2.0 * (1 << zoom));

        tilesLoaded = 0;
        totalTilesToLoad = gridSize * gridSize;

        // Load 3x3 grid of tiles
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                final int gridX = dx + 1;
                final int gridY = dy + 1;
                final int tileX = centerTileX + dx;
                final int tileY = centerTileY + dy;

                loadTile(tileX, tileY, gridX, gridY);
            }
        }
    }

    private void loadTile(final int tileX, final int tileY, final int gridX, final int gridY) {
        String key = zoom + "_" + tileX + "_" + tileY;

        // Geoapify tile URL - using @2x for retina display (512x512 pixels)
        // Note: Standard tiles are z/x/y format
        String tileUrl = String.format(
            "https://maps.geoapify.com/v1/tile/osm-bright/%d/%d/%d.png?apiKey=%s",
            zoom, tileX, tileY, GEOAPIFY_API_KEY
        );

        // Use cached tile if available
        Texture cached = tileCache.get(key);
        if (cached != null) {
            mapTiles[gridX][gridY] = cached;
            tilesLoaded++;
            return;
        }

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(tileUrl)
            .header("Accept", "image/png")
            .build();

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int statusCode = httpResponse.getStatus().getStatusCode();

                    if (statusCode != 200) {
                        String result = httpResponse.getResultAsString();
                        Gdx.app.error("MapRenderer", "HTTP " + statusCode + " for tile " + tileX + "," + tileY);
                        Gdx.app.error("MapRenderer", "Response: " + result);
                        tilesLoaded++;
                        return;
                    }

                    byte[] imageData = httpResponse.getResult();

                    if (imageData == null || imageData.length == 0) {
                        Gdx.app.error("MapRenderer", "Empty image data for tile " + tileX + "," + tileY);
                        tilesLoaded++;
                        return;
                    }

                    Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);

                    Gdx.app.postRunnable(() -> {
                        try {
                            Texture tex = new Texture(pixmap);
                            tileCache.put(key, tex);
                            mapTiles[gridX][gridY] = tex;
                            Gdx.app.log("MapRenderer", "Successfully loaded tile " + tileX + "," + tileY);
                        } finally {
                            pixmap.dispose();
                        }

                        tilesLoaded++;
                        if (tilesLoaded >= totalTilesToLoad) {
                            isLoading = false;
                        }
                    });

                } catch (Exception e) {
                    Gdx.app.error("MapRenderer", "Error loading tile " + tileX + "," + tileY + ": " + e.getMessage());
                    e.printStackTrace();
                    tilesLoaded++;
                    if (tilesLoaded >= totalTilesToLoad) {
                        isLoading = false;
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("MapRenderer", "Failed to load tile " + tileX + "," + tileY + ": " + t.getMessage());
                t.printStackTrace();
                tilesLoaded++;
                if (tilesLoaded >= totalTilesToLoad) {
                    isLoading = false;
                }
            }

            @Override
            public void cancelled() {
                tilesLoaded++;
                if (tilesLoaded >= totalTilesToLoad) {
                    isLoading = false;
                }
            }
        });
    }

    public void render(SpriteBatch batch, float screenX, float screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Calculate tile position to center the map with pan offset
        float startX = (screenWidth - (gridSize * tileSize)) / 2f + panOffsetX;
        float startY = (screenHeight - (gridSize * tileSize)) / 2f + panOffsetY;

        // Draw tiles
        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                if (mapTiles[gx][gy] != null) {
                    float x = startX + (gx * tileSize);
                    float y = startY + ((gridSize - 1 - gy) * tileSize);
                    batch.draw(mapTiles[gx][gy], x, y, tileSize, tileSize);
                }
            }
        }

        batch.end();

        // Draw markers on top
        drawMarkers(startX, startY);

        batch.begin();
    }

    private void drawMarkers(float mapStartX, float mapStartY) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
            com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (WMSDataFetcher.LocationData marker : markers) {
            // Convert lat/lon to tile coordinates
            double markerTileX = (marker.longitude + 180.0) / 360.0 * (1 << zoom);
            double markerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(marker.latitude)) +
                1.0 / Math.cos(Math.toRadians(marker.latitude))) / Math.PI) / 2.0 * (1 << zoom);

            // Calculate pixel position relative to center tile
            double pixelX = (markerTileX - (centerTileX - 1)) * tileSize;
            double pixelY = (markerTileY - (centerTileY - 1)) * tileSize;

            float screenX = mapStartX + (float) pixelX;
            float screenY = mapStartY + (gridSize * tileSize) - (float) pixelY;

            // Draw marker circle
            Color color = getMarkerColor(marker.type);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(screenX, screenY, 8);

            // Draw white border
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(screenX, screenY, 9);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(screenX, screenY, 7);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private Color getMarkerColor(String type) {
        switch (type) {
            case "kindergarten":  return new Color(0, 0, 1, 0.8f); // Blue
            case "playground":   return new Color(0, 1, 0, 0.8f); // Green
            case "train":  return new Color(1, 0, 0, 0.8f); // Red
            default: return new Color(0.5f, 0.5f, 0.5f, 0.8f); // Gray
        }
    }

    public void addMarkers(Array<WMSDataFetcher.LocationData> newMarkers) {
        markers.addAll(newMarkers);
    }

    public void clearMarkers() {
        markers.clear();
    }

    public void setCenter(double lat, double lon) {
        this.centerLat = lat;
        this.centerLon = lon;
    }

    public void setZoom(int zoom) { this.zoom = Math.max(1, Math.min(19, zoom)); }

    public boolean isMapLoaded() {
        return tilesLoaded >= totalTilesToLoad;
    }

    public Array<WMSDataFetcher.LocationData> getMarkers() {
        return markers;
    }

    public void dispose() {
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                if (mapTiles[x][y] != null) {
                    mapTiles[x][y].dispose();
                }
            }
        }
        shapeRenderer.dispose();
    }
}
