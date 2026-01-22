package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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

    public RenderSystem(MapComponent map, TileComponent tiles,
                        MarkerComponent markers, UIComponent ui) {
        this.map = map;
        this.tiles = tiles;
        this.markers = markers;
        this.ui = ui;

        titleFont.getData().setScale(2f);
        font.getData().setScale(1.2f);
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

    private void renderMarkers() {
        shapes.setProjectionMatrix(map.camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        renderApiMarkers();
        renderCustomMarkers();

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

    private void renderApiMarkers() {
        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 pos = markers.markerPositions.get(i);
            WMSDataFetcher.LocationData data = markers.markers.get(i);

            MarkerType type = toMarkerType(data.type);
            if (type == null) continue;
            if (!ui.activeFilters.contains(type)) continue;

            boolean isSelected = markers.selected == data;
            boolean isHovered = markers.hovered == data;

            // Color
            switch (type) {
                case KINDERGARTEN: shapes.setColor(0.2f, 0.6f, 1f, 1f); break;
                case PLAYGROUND:   shapes.setColor(0.2f, 0.8f, 0.2f, 1f); break;
                case TRAIN:        shapes.setColor(1f, 0.5f, 0f, 1f); break;
                default:           shapes.setColor(Color.RED);
            }

            float size = isSelected ? 10f : isHovered ? 8f : 6f;
            shapes.circle(pos.x, pos.y, size);

            if (isSelected || isHovered) drawMarkerOutline(pos, size);
        }
    }

    private void renderCustomMarkers() {
        if (!ui.activeFilters.contains(MarkerType.CUSTOM)) return;

        for (int i = 0; i < markers.customMarkerPositions.size; i++) {
            Vector2 pos = markers.customMarkerPositions.get(i);
            WMSDataFetcher.LocationData data = markers.customMarkers.get(i);

            boolean isSelected = markers.selected == data;
            boolean isHovered = markers.hovered == data;

            shapes.setColor(0.7f, 0.2f, 1f, 1f);

            float size = isSelected ? 11f : isHovered ? 9f : 7f;
            shapes.circle(pos.x, pos.y, size);

            if (isSelected || isHovered) drawMarkerOutline(pos, size);
        }
    }

    private void drawMarkerOutline(Vector2 pos, float size) {
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.WHITE);
        Gdx.gl.glLineWidth(2);
        shapes.circle(pos.x, pos.y, size);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private void renderTrains() {
        if (!ui.activeFilters.contains(MarkerType.TRAIN)) return;
        
        shapes.setProjectionMatrix(map.camera.combined);

        // Filled circles
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.8f, 0f, 1f);

        Vector2 t1 = getTrainPosition(1);
        if (t1 != null) shapes.circle(t1.x, t1.y, 12);

        Vector2 t2 = getTrainPosition(2);
        if (t2 != null) shapes.circle(t2.x, t2.y, 12);

        shapes.end();

        // Outlines
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(1f, 0.5f, 0f, 1f);

        if (t1 != null) shapes.circle(t1.x, t1.y, 12);
        if (t2 != null) shapes.circle(t2.x, t2.y, 12);

        shapes.end();
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
        
        renderTrainRoutes();
        renderMarkers();
        renderTrains();
        
        batch.begin();
    }

    // ---------------------------------------------------------
    // TRAIN POSITION
    // ---------------------------------------------------------

    private Vector2 getPositionOnRoute(MarkerComponent markers, int startIdx, int endIdx, float progress) {
        if (startIdx >= endIdx || markers.trainRouteWorldCoords.size <= endIdx) return null;

        int routeLength = endIdx - startIdx + 1;
        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        int index = startIdx + (int)(clampedProgress * (routeLength - 1));
        float fraction = (clampedProgress * (routeLength - 1)) - (index - startIdx);

        // Ne interpoliraj čez separator
        if (markers.route1SeparatorIndex >= 0) {
            if (index == markers.route1SeparatorIndex ||
                index + 1 == markers.route1SeparatorIndex) {
                return null;
            }
        }

        if (index >= markers.trainRouteWorldCoords.size - 1)
            return markers.trainRouteWorldCoords.get(markers.trainRouteWorldCoords.size - 1);

        Vector2 p1 = markers.trainRouteWorldCoords.get(index);
        Vector2 p2 = markers.trainRouteWorldCoords.get(index + 1);

        if (p1.x < -999000 || p2.x < -999000) return null;

        return new Vector2(
            p1.x + (p2.x - p1.x) * fraction,
            p1.y + (p2.y - p1.y) * fraction
        );
    }
}

