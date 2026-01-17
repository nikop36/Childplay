package si.um.feri.rri.component;

import com.badlogic.gdx.graphics.Texture;

public class TileComponent {

    public static class Tile {
        public int tileX;
        public int tileY;
        public float worldX;
        public float worldY;
        public Texture texture;
    }

    public Tile[][] tiles;
    public boolean loaded = false;
    public int totalTiles;
    public int loadedTiles;
}
