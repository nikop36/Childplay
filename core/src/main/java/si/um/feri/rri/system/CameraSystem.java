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
        map.camera.zoom = MathUtils.clamp(map.camera.zoom, 0.5f, 3f);
        clampCamera();
    }

    private void clampCamera() {
        float halfW = map.camera.viewportWidth * map.camera.zoom / 2f;
        float halfH = map.camera.viewportHeight * map.camera.zoom / 2f;

        map.camera.position.x = MathUtils.clamp(map.camera.position.x, halfW, map.worldWidth - halfW);
        map.camera.position.y = MathUtils.clamp(map.camera.position.y, halfH, map.worldHeight - halfH);
    }
}
