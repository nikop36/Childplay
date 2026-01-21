package si.um.feri.rri.old;

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

    public void render(SpriteBatch batch) {

        if (!tiles.loaded) {
            batch.setProjectionMatrix(batch.getProjectionMatrix().idt());
            int w = Gdx.graphics.getWidth();
            int h = Gdx.graphics.getHeight();
            titleFont.draw(batch, "Loading map...", w / 2f - 100, h / 2f);
            return;
        }


        // world-space rendering
        batch.setProjectionMatrix(map.camera.combined);

        int gridSize = map.gridSize;

        if (tiles.tiles != null) {
            for (int gy = 0; gy < gridSize; gy++) {
                for (int gx = 0; gx < gridSize; gx++) {
                    TileComponent.Tile t = tiles.tiles[gx][gy];
                    if (t == null || t.texture == null) continue;

                    batch.draw(t.texture, t.worldX, t.worldY, map.tileSize, map.tileSize);
                }
            }
        }

        // End batch before drawing shapes
        batch.end();

        // Draw train route
        if (markers.trainRouteWorldCoords.size > 1) {
            shapes.setProjectionMatrix(map.camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(1.0f, 0.5f, 0.0f, 0.8f);

            for (int i = 0; i < markers.trainRouteWorldCoords.size - 1; i++) {
                Vector2 p1 = markers.trainRouteWorldCoords.get(i);
                Vector2 p2 = markers.trainRouteWorldCoords.get(i + 1);

                // Skip if we hit the separator marker
                if (p1.x < -999000 || p2.x < -999000) {
                    continue;
                }

                shapes.rectLine(p1.x, p1.y, p2.x, p2.y, 3);
            }

            shapes.end();
        }

        // markers
        shapes.setProjectionMatrix(map.camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 pos = markers.markerPositions.get(i);
            String type = markers.markers.get(i).type;


            boolean isSelected = markers.selected != null && markers.markers.get(i) == markers.selected;
            boolean isHovered = markers.hovered != null && markers.markers.get(i) == markers.hovered;

            // Set color based on type
            if (type.equals("kindergarten")) {
                shapes.setColor(0.2f, 0.6f, 1.0f, 1.0f); // Blue
            } else if (type.equals("playground")) {
                shapes.setColor(0.2f, 0.8f, 0.2f, 1.0f); // Green
            } else if (type.equals("train")) {
                shapes.setColor(1.0f, 0.5f, 0.0f, 1.0f); // Orange
            } else {
                shapes.setColor(Color.RED);
            }

            float size = 6f;
            if (isSelected) {
                size = 10f;
            } else if (isHovered) {
                size = 8f;
            }

            shapes.circle(pos.x, pos.y, size);

            // Draw outline for selected/hovered
            if (isSelected || isHovered) {
                shapes.end();
                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(Color.WHITE);
                Gdx.gl.glLineWidth(2);
                shapes.circle(pos.x, pos.y, size);
                shapes.end();
                shapes.begin(ShapeRenderer.ShapeType.Filled);
            }
        }

        // Draw animated trains
        // Train 1 on first route
        if (markers.route1SeparatorIndex > 1) {
            int routeSize = markers.route1SeparatorIndex;
            Vector2 train1Pos = getPositionOnRoute(markers, 0, routeSize - 1, markers.train1Progress);
            if (train1Pos != null) {
                shapes.setColor(1.0f, 0.8f, 0.0f, 1.0f); // Bright yellow/gold
                shapes.circle(train1Pos.x, train1Pos.y, 12);
            }
        }

        // Train 2 on second route
        if (markers.route1SeparatorIndex >= 0 && markers.trainRouteWorldCoords.size > markers.route1SeparatorIndex + 2) {
            int startIdx = markers.route1SeparatorIndex + 1;
            int endIdx = markers.trainRouteWorldCoords.size - 1;
            Vector2 train2Pos = getPositionOnRoute(markers, startIdx, endIdx, markers.train2Progress);
            if (train2Pos != null) {
                shapes.setColor(1.0f, 0.8f, 0.0f, 1.0f);
                shapes.circle(train2Pos.x, train2Pos.y, 12);
            }
        }

        shapes.end();

        // Draw train outlines
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setProjectionMatrix(map.camera.combined);
        Gdx.gl.glLineWidth(3);

        if (markers.route1SeparatorIndex > 1) {
            int routeSize = markers.route1SeparatorIndex;
            Vector2 train1Pos = getPositionOnRoute(markers, 0, routeSize - 1, markers.train1Progress);
            if (train1Pos != null) {
                shapes.setColor(1.0f, 0.5f, 0.0f, 1.0f);
                shapes.circle(train1Pos.x, train1Pos.y, 12);
            }
        }

        if (markers.route1SeparatorIndex >= 0 && markers.trainRouteWorldCoords.size > markers.route1SeparatorIndex + 2) {
            int startIdx = markers.route1SeparatorIndex + 1;
            int endIdx = markers.trainRouteWorldCoords.size - 1;
            Vector2 train2Pos = getPositionOnRoute(markers, startIdx, endIdx, markers.train2Progress);
            if (train2Pos != null) {
                shapes.setColor(1.0f, 0.5f, 0.0f, 1.0f);
                shapes.circle(train2Pos.x, train2Pos.y, 12);
            }
        }

        shapes.end();

        // Resume batch for UI
        batch.begin();

        // UI in screen space
        batch.setProjectionMatrix(batch.getProjectionMatrix().idt());
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        titleFont.draw(batch, "Childplay Maribor", 30, h - 30);
        font.draw(batch, "Kindergartens: " + ui.kindergartens, 60, 135);
        font.draw(batch, "Playgrounds: " + ui.playgrounds, 60, 100);
        font.draw(batch, "Train Stops: " + ui.trainStops, 60, 65);

        if (markers.selected != null) {
            int infoX = w - 400;
            int infoY = h - 100;
            int lineHeight = 25;

            batch.end();

            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0, 0, 0, 0.7f);
            shapes.rect(infoX - 10, infoY - 80, 380, 100);
            shapes.end();

            batch.begin();
            titleFont.draw(batch, markers.selected.name, infoX, infoY);
            font.draw(batch, "Type: " + markers.selected.type, infoX, infoY - lineHeight);
            font.draw(batch, "Location: " + String.format("%.4f, %.4f",
                markers.selected.latitude, markers.selected.longitude), infoX, infoY - lineHeight * 2);
        }
    }

    private Vector2 getPositionOnRoute(MarkerComponent markers, int startIdx, int endIdx, float progress) {
        if (startIdx >= endIdx || markers.trainRouteWorldCoords.size <= endIdx) {
            return null;
        }

        int routeLength = endIdx - startIdx + 1;
        int index = startIdx + (int)(progress * (routeLength - 1));
        float fraction = (progress * (routeLength - 1)) - (index - startIdx);

        if (index >= markers.trainRouteWorldCoords.size - 1) {
            return markers.trainRouteWorldCoords.get(markers.trainRouteWorldCoords.size - 1);
        }

        Vector2 p1 = markers.trainRouteWorldCoords.get(index);
        Vector2 p2 = markers.trainRouteWorldCoords.get(index + 1);

        // Skip separator points
        if (p1.x < -999000 || p2.x < -999000) {
            return null;
        }

        return new Vector2(
            p1.x + (p2.x - p1.x) * fraction,
            p1.y + (p2.y - p1.y) * fraction
        );
    }

    public void updateTrains(float deltaTime) {
        float speed = 0.05f; // Adjust speed here

        // Update train 1
        if (markers.train1Forward) {
            markers.train1Progress += speed * deltaTime;
            if (markers.train1Progress >= 1.0f) {
                markers.train1Progress = 1.0f;
                markers.train1Forward = false;
            }
        } else {
            markers.train1Progress -= speed * deltaTime;
            if (markers.train1Progress <= 0.0f) {
                markers.train1Progress = 0.0f;
                markers.train1Forward = true;
            }
        }

        // Update train 2
        if (markers.train2Forward) {
            markers.train2Progress += speed * deltaTime;
            if (markers.train2Progress >= 1.0f) {
                markers.train2Progress = 1.0f;
                markers.train2Forward = false;
            }
        } else {
            markers.train2Progress -= speed * deltaTime;
            if (markers.train2Progress <= 0.0f) {
                markers.train2Progress = 0.0f;
                markers.train2Forward = true;
            }
        }
    }
}
