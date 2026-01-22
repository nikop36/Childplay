package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.TileComponent;
import si.um.feri.rri.component.UIComponent;
import si.um.feri.rri.component.enums.MarkerType;
import si.um.feri.rri.services.WMSDataFetcher;


public class RenderSystem {

    private final MapComponent map;
    private final TileComponent tiles;
    private final MarkerComponent markers;
    private final UIComponent ui;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final BitmapFont titleFont = new BitmapFont();

    // Marker icon textures
    private final Texture iconKindergarten;
    private final Texture iconPlayground;
    private final Texture iconTrain;
    private final Texture iconTrainMoving;

    public RenderSystem(MapComponent map, TileComponent tiles,
                        MarkerComponent markers, UIComponent ui) {
        this.map = map;
        this.tiles = tiles;
        this.markers = markers;
        this.ui = ui;

        titleFont.getData().setScale(2f);
        font.getData().setScale(1.2f);

        // Load icons from assets/ui/icons/ with fallbacks
        Texture fallback = createFallbackTexture();

        Texture t;
        try {
            t = new Texture(Gdx.files.internal("ui/icons/kindergarden.png"));
        } catch (Exception e) {
            Gdx.app.error("RenderSystem", "Failed to load kindergarden icon: " + e.getMessage());
            t = fallback;
        }
        iconKindergarten = t;

        try {
            t = new Texture(Gdx.files.internal("ui/icons/playground.png"));
        } catch (Exception e) {
            Gdx.app.error("RenderSystem", "Failed to load playground icon: " + e.getMessage());
            t = fallback;
        }
        iconPlayground = t;

        try {
            t = new Texture(Gdx.files.internal("ui/icons/train.png"));
        } catch (Exception e) {
            Gdx.app.error("RenderSystem", "Failed to load train icon: " + e.getMessage());
            t = fallback;
        }
        iconTrain = t;

        try {
            t = new Texture(Gdx.files.internal("ui/icons/train_moving.png"));
        } catch (Exception e) {
            Gdx.app.error("RenderSystem", "Failed to load train_moving icon: " + e.getMessage());
            t = fallback;
        }
        iconTrainMoving = t;
    }

    // ---------------------------------------------------------
    // LOADING SCREEN
    // ---------------------------------------------------------

    private void renderLoadingScreen(SpriteBatch batch) {
        batch.setProjectionMatrix(batch.getProjectionMatrix().idt());

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        titleFont.draw(batch, "Loading map...", w / 2f - 100, h / 2f);
    }


    // ---------------------------------------------------------
    // WORLD SPACE RENDERING
    // ---------------------------------------------------------

    private void renderTiles(SpriteBatch batch) {
        int gridSize = map.gridSize;

        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                TileComponent.Tile t = tiles.tiles[gx][gy];
                if (t == null || t.texture == null) continue;

                batch.draw(t.texture, t.worldX, t.worldY, map.tileSize, map.tileSize);
            }
        }
    }

    private void renderTrainRoutes() {
        if (!ui.activeFilters.contains(MarkerType.TRAIN)) return;
        if (markers.trainRouteWorldCoords.size <= 1) return;

        shapes.setProjectionMatrix(map.camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.5f, 0f, 0.8f);

        for (int i = 0; i < markers.trainRouteWorldCoords.size - 1; i++) {
            Vector2 p1 = markers.trainRouteWorldCoords.get(i);
            Vector2 p2 = markers.trainRouteWorldCoords.get(i + 1);

            if (p1.x < -999000 || p2.x < -999000) continue;

            shapes.rectLine(p1.x, p1.y, p2.x, p2.y, 3);
        }

        shapes.end();
    }

    private MarkerType toMarkerType(String type) {
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "kindergarten": return MarkerType.KINDERGARTEN;
            case "playground":   return MarkerType.PLAYGROUND;
            case "train":        return MarkerType.TRAIN;
            case "custom":       return MarkerType.CUSTOM;
            default:             return null;
        }
    }

    private void renderMarkerIcons(SpriteBatch batch) {
        // Draw API markers using icons
        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 pos = markers.markerPositions.get(i);
            WMSDataFetcher.LocationData data = markers.markers.get(i);

            MarkerType type = toMarkerType(data.type);
            if (type == null) continue;
            if (!ui.activeFilters.contains(type)) continue;

            boolean isSelected = markers.selected == data;
            boolean isHovered = markers.hovered == data;

            // Make icons smaller: normal / hovered / selected
            float baseSize = 24f;
            if (isHovered) baseSize = 30f;
            if (isSelected) baseSize = 36f;

            Texture tex = null;
            switch (type) {
                case KINDERGARTEN: tex = iconKindergarten; break;
                case PLAYGROUND:   tex = iconPlayground;   break;
                case TRAIN:        tex = iconTrain;        break;
                default:           tex = iconTrain;        break;
            }

            batch.draw(tex, pos.x - baseSize / 2f, pos.y - baseSize / 2f, baseSize, baseSize);

            if (isSelected || isHovered) {
                // draw outline using shapes; end batch temporarily
                batch.end();
                drawMarkerOutline(pos, baseSize / 2f + 4f);
                batch.begin();
            }
        }

        // Custom markers (use a magenta circle or reuse an icon)
        if (ui.activeFilters.contains(MarkerType.CUSTOM)) {
            for (int i = 0; i < markers.customMarkerPositions.size; i++) {
                Vector2 pos = markers.customMarkerPositions.get(i);
                WMSDataFetcher.LocationData data = markers.customMarkers.get(i);

                boolean isSelected = markers.selected == data;
                boolean isHovered = markers.hovered == data;

                // Make custom icons smaller too
                float baseSize = 24f;
                if (isHovered) baseSize = 30f;
                if (isSelected) baseSize = 36f;

                // fallback icon for custom markers
                batch.draw(iconTrain, pos.x - baseSize / 2f, pos.y - baseSize / 2f, baseSize, baseSize);

                if (isSelected || isHovered) {
                    batch.end();
                    drawMarkerOutline(pos, baseSize / 2f + 4f);
                    batch.begin();
                }
            }
        }
    }

    private void drawMarkerOutline(Vector2 pos, float radius) {
        shapes.setProjectionMatrix(map.camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.WHITE);
        try {
            Gdx.gl.glLineWidth(2);
        } catch (Exception ignored) {}
        shapes.circle(pos.x, pos.y, radius);
        shapes.end();
    }

    private void renderTrains(SpriteBatch batch) {
        if (!ui.activeFilters.contains(MarkerType.TRAIN)) return;

        // Draw train icons at interpolated positions. Begin/end the batch here because
        // renderWorld wraps batch.begin()/end around other sections.
        float iconSize = 28f; // slightly larger than marker icons

        Vector2 t1 = getTrainPosition(1);
        Vector2 t2 = getTrainPosition(2);

        batch.begin();
        if (t1 != null) {
            batch.draw(iconTrainMoving, t1.x - iconSize / 2f, t1.y - iconSize / 2f, iconSize, iconSize);
        }
        if (t2 != null) {
            batch.draw(iconTrainMoving, t2.x - iconSize / 2f, t2.y - iconSize / 2f, iconSize, iconSize);
        }
        batch.end();
    }

    private Vector2 getTrainPosition(int train) {
        if (train == 1 && markers.route1SeparatorIndex > 1) {
            return getPositionOnRoute(markers, 0, markers.route1SeparatorIndex - 1, markers.train1Progress);
        }

        if (train == 2 &&
            markers.route1SeparatorIndex >= 0 &&
            markers.trainRouteWorldCoords.size > markers.route1SeparatorIndex + 2) {

            return getPositionOnRoute(markers,
                markers.route1SeparatorIndex + 1,
                markers.trainRouteWorldCoords.size - 1,
                markers.train2Progress);
        }

        return null;
    }

    private Vector2 getPositionOnRoute(MarkerComponent markers, int startIdx, int endIdx, float progress) {
        // Validate indices and route length
        if (startIdx >= endIdx) return null;
        if (markers.trainRouteWorldCoords.size <= startIdx) return null;
        // Ensure endIdx is within bounds
        int safeEnd = Math.min(endIdx, markers.trainRouteWorldCoords.size - 1);
        int routeLength = safeEnd - startIdx + 1;
        if (routeLength < 2) return null; // need at least two points to interpolate

        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        // Compute a base index in [startIdx, safeEnd]
        int rawIndex = startIdx + (int)(clampedProgress * (routeLength - 1));
        // Clamp index so index+1 is valid (<= safeEnd-1)
        int index = Math.min(rawIndex, safeEnd - 1);
        // Recompute fraction relative to the chosen segment
        float fraction;
        if (routeLength == 1) {
            fraction = 0f;
        } else {
            float segmentCount = (routeLength - 1);
            float progressPos = clampedProgress * segmentCount;
            fraction = progressPos - (index - startIdx);
        }

        // Avoid separator
        if (markers.route1SeparatorIndex >= 0) {
            if (index == markers.route1SeparatorIndex || index + 1 == markers.route1SeparatorIndex) {
                return null;
            }
        }

        Vector2 p1 = markers.trainRouteWorldCoords.get(index);
        Vector2 p2 = markers.trainRouteWorldCoords.get(index + 1);

        if (p1.x < -999000 || p2.x < -999000) return null;

        return new Vector2(
            p1.x + (p2.x - p1.x) * fraction,
            p1.y + (p2.y - p1.y) * fraction
        );
    }

    // ---------------------------------------------------------
    // UI RENDERING
    // ---------------------------------------------------------

    public void renderWorld(SpriteBatch batch) {
        if (!tiles.loaded) {
            renderLoadingScreen(batch);
            return;
        }

        batch.setProjectionMatrix(map.camera.combined);

        renderTiles(batch);
        batch.end();

        // Train routes use ShapeRenderer
        renderTrainRoutes();

        // Draw icons with SpriteBatch
        batch.begin();
        renderMarkerIcons(batch);
        batch.end();

        // Trains as icons
        renderTrains(batch);

        batch.begin();
    }

    private Texture createFallbackTexture() {
        Pixmap pix = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
        pix.setColor(1f, 0f, 1f, 1f); // magenta to be noticeable
        pix.fill();
        Texture tex = new Texture(pix);
        pix.dispose();
        return tex;
    }
}
