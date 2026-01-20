package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.services.WMSDataFetcher;

public class InputSystem implements InputProcessor {

    private final CameraSystem cameraSystem;
    private final MapComponent map;
    private final MarkerComponent markers;

    private float lastX, lastY;
    private boolean dragging = false;
    private boolean clickedOnMarker = false;
    private static final float CLICK_RADIUS = 12f;

    public InputSystem(CameraSystem cameraSystem, MapComponent map, MarkerComponent markers) {
        this.cameraSystem = cameraSystem;
        this.map = map;
        this.markers = markers;
    }
    
    public void update() {
        updateHoveredMarker();
    }
    
    private void updateHoveredMarker() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        
        Vector3 worldCoords = map.camera.unproject(new Vector3(mouseX, mouseY, 0));
        Vector2 mousePos = new Vector2(worldCoords.x, worldCoords.y);
        
        markers.hovered = null;
        
        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 markerPos = markers.markerPositions.get(i);
            float distance = mousePos.dst(markerPos);
            
            if (distance < CLICK_RADIUS) {
                markers.hovered = markers.markers.get(i);
                return;
            }
        }
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        cameraSystem.zoom(amountY);
        return true;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        lastX = x;
        lastY = y;
        dragging = false;
        clickedOnMarker = isClickOnMarker(x, y);
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        if (clickedOnMarker) {
            return true;
        }

        dragging = true;
        float dx = x - lastX;
        float dy = y - lastY;

        cameraSystem.pan(dx, dy);

        lastX = x;
        lastY = y;
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (clickedOnMarker && !dragging) {
            checkMarkerClick(x, y);
        }
        dragging = false;
        clickedOnMarker = false;
        return true;
    }

    private boolean isClickOnMarker(int screenX, int screenY) {
        Vector3 worldCoords = map.camera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 clickPos = new Vector2(worldCoords.x, worldCoords.y);

        for (Vector2 markerPos : markers.markerPositions) {
            float distance = clickPos.dst(markerPos);
            if (distance < CLICK_RADIUS) {
                return true;
            }
        }
        return false;
    }

    private void checkMarkerClick(int screenX, int screenY) {
        Vector3 worldCoords = map.camera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 clickPos = new Vector2(worldCoords.x, worldCoords.y);

        markers.selected = null;

        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 markerPos = markers.markerPositions.get(i);
            float distance = clickPos.dst(markerPos);

            if (distance < CLICK_RADIUS) {
                markers.selected = markers.markers.get(i);
                com.badlogic.gdx.Gdx.app.log("InputSystem", "Clicked marker: " + markers.selected.name);
                return;
            }
        }
    }

    // unused
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int i, int i1, int i2, int i3) { return false; }
}
