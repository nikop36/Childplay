package si.um.feri.rri.system;

import com.badlogic.gdx.math.MathUtils;
import si.um.feri.rri.component.MapComponent;

public class CameraSystem {

    private final MapComponent map;

    public CameraSystem(MapComponent map) {
        this.map = map;
    }

    public void pan(float dx, float dy) {
        map.camera.translate(-dx * map.camera.zoom, dy * map.camera.zoom);
        clampCamera();
    }

    public void zoom(float amount) {
        map.camera.zoom += amount * 0.1f;
        map.camera.zoom = MathUtils.clamp(map.camera.zoom, 0.5f, 1.0f);
        clampCamera();
    }

    private void clampCamera() {
        if (map.worldWidth <= 0 || map.worldHeight <= 0) {
            return;
        }
        
        float halfW = map.camera.viewportWidth * map.camera.zoom / 2f;
        float halfH = map.camera.viewportHeight * map.camera.zoom / 2f;

        // If zoomed out so much that viewport is larger than world, keep camera centered
        if (halfW * 2 >= map.worldWidth || halfH * 2 >= map.worldHeight) {
            map.camera.position.x = map.worldMinX + map.worldWidth / 2f;
            map.camera.position.y = map.worldMinY + map.worldHeight / 2f;
            return;
        }

        float minX = map.worldMinX + halfW;
        float maxX = map.worldMinX + map.worldWidth - halfW;
        float minY = map.worldMinY + halfH;
        float maxY = map.worldMinY + map.worldHeight - halfH;

        map.camera.position.x = MathUtils.clamp(map.camera.position.x, minX, maxX);
        map.camera.position.y = MathUtils.clamp(map.camera.position.y, minY, maxY);
    }
}
