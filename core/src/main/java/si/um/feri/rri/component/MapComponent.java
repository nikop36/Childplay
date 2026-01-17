package si.um.feri.rri.component;

import com.badlogic.gdx.graphics.OrthographicCamera;

public class MapComponent {
    public OrthographicCamera camera;

    public int tileSize = 256;
    public int gridSize = 5;

    public float worldWidth;
    public float worldHeight;

    public int centerTileX = 4400;
    public int centerTileY = 2860;
    public int zoom = 13;
}
