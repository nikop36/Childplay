package si.um.feri.rri.system;

import com.badlogic.gdx.InputProcessor;

public class InputSystem implements InputProcessor {

    private final CameraSystem cameraSystem;

    private float lastX, lastY;
    private boolean dragging = false;

    public InputSystem(CameraSystem cameraSystem) {
        this.cameraSystem = cameraSystem;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        cameraSystem.zoom(amountY);
        return true;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        dragging = true;
        lastX = x;
        lastY = y;
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        if (!dragging) return false;

        float dx = x - lastX;
        float dy = y - lastY;

        cameraSystem.pan(dx, dy);

        lastX = x;
        lastY = y;
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        dragging = false;
        return true;
    }

    // unused
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int i, int i1, int i2, int i3) { return false; }
}
