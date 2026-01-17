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
            int w = com.badlogic.gdx.Gdx.graphics.getWidth();
            int h = com.badlogic.gdx.Gdx.graphics.getHeight();
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

        // markers
        shapes.setProjectionMatrix(map.camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (Vector2 pos : markers.markerPositions) {
            shapes.setColor(Color.RED);
            shapes.circle(pos.x, pos.y, 10);
        }

        shapes.end();

        // UI in screen space
        batch.setProjectionMatrix(batch.getProjectionMatrix().idt());
        int w = com.badlogic.gdx.Gdx.graphics.getWidth();
        int h = com.badlogic.gdx.Gdx.graphics.getHeight();

        titleFont.draw(batch, "Childplay Maribor", 30, h - 30);
        font.draw(batch, "Kindergartens: " + ui.kindergartens, 60, 135);
        font.draw(batch, "Playgrounds: " + ui.playgrounds, 60, 100);
        font.draw(batch, "Train Stops: " + ui.trainStops, 60, 65);
    }
}
