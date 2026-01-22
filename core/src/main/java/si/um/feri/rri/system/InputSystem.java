package si.um.feri.rri.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import si.um.feri.rri.component.MapComponent;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.UIComponent;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;


import java.awt.*;

public class InputSystem implements InputProcessor {

    private final CameraSystem cameraSystem;
    private final MapComponent map;
    private final MarkerComponent markers;
    private final UIComponent ui;
    private final Stage stage;

    private float lastX, lastY;
    private boolean dragging = false;
    private boolean clickedOnMarker = false;

    private static final float CLICK_RADIUS = 12f;

    public InputSystem(CameraSystem cameraSystem, MapComponent map,
                       MarkerComponent markers, UIComponent ui, Stage stage) {
        this.cameraSystem = cameraSystem;
        this.map = map;
        this.markers = markers;
        this.ui = ui;
        this.stage = stage;
    }

    // Called every frame
    public void update() {
        updateHoveredMarker();
    }

    // -----------------------------
    // HOVER DETECTION
    // -----------------------------
    private void updateHoveredMarker() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();

        Vector3 worldCoords = map.camera.unproject(new Vector3(mouseX, mouseY, 0));
        Vector2 mousePos = new Vector2(worldCoords.x, worldCoords.y);

        markers.hovered = null;

        // Custom markers first
        for (int i = 0; i < markers.customMarkerPositions.size; i++) {
            Vector2 pos = markers.customMarkerPositions.get(i);
            if (mousePos.dst(pos) < CLICK_RADIUS) {
                markers.hovered = markers.customMarkers.get(i);
                return;
            }
        }

        // API markers
        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 pos = markers.markerPositions.get(i);
            if (mousePos.dst(pos) < CLICK_RADIUS) {
                markers.hovered = markers.markers.get(i);
                return;
            }
        }
    }

    // -----------------------------
    // SCROLL (ZOOM)
    // -----------------------------
    @Override
    public boolean scrolled(float amountX, float amountY) {
        cameraSystem.zoom(amountY);
        return true;
    }

    // -----------------------------
    // TOUCH DOWN
    // -----------------------------
    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (ui.waitingForPlacement) return false;

        if (stage.hit(x, Gdx.graphics.getHeight() - y, true) != null) {
            return false;
        }

        lastX = x;
        lastY = y;
        dragging = false;
        clickedOnMarker = isClickOnMarker(x, y);
        return true;
    }

    // -----------------------------
    // DRAG (PAN CAMERA)
    // -----------------------------
    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        if (ui.waitingForPlacement) return false;

        if (clickedOnMarker) return true;

        dragging = true;
        float dx = x - lastX;
        float dy = y - lastY;

        cameraSystem.pan(dx, dy);

        lastX = x;
        lastY = y;
        return true;
    }

    // -----------------------------
    // TOUCH UP (CLICK)
    // -----------------------------
    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        // If we are in placement mode, place marker on map click
        if (ui.waitingForPlacement) {

            Vector3 world = map.camera.unproject(new Vector3(x, y, 0));

            ui.newMarkerWorldX = world.x;
            ui.newMarkerWorldY = world.y;

            System.out.println("camera.zoom = " + map.camera.zoom);
            System.out.println("world.x/map.camera.zoom = " + world.x / map.camera.zoom);
            System.out.println("world.y/map.camera.zoom = " + world.y / map.camera.zoom);

            ui.newMarkerName = ui.pendingMarkerName;
            ui.newMarkerType = ui.pendingMarkerType;

            ui.addMarkerRequested = true;
            ui.waitingForPlacement = false;

            return true;
        }

        if (stage.hit(x, Gdx.graphics.getHeight() - y, true) != null) {
            return false;
        }

        // UI windows first
        if (ui.showAddWindow) {
            handleAddWindowClick(x, y);
            return true;
        }

        if (ui.showEditWindow) {
            handleEditWindowClick(x, y);
            return true;
        }

        // Normal marker click
        if (clickedOnMarker && !dragging) {
            checkMarkerClick(x, y);
        }

        dragging = false;
        clickedOnMarker = false;
        return true;
    }

    // -----------------------------
    // CHECK IF CLICK IS ON MARKER
    // -----------------------------
    private boolean isClickOnMarker(int screenX, int screenY) {
        Vector3 worldCoords = map.camera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 clickPos = new Vector2(worldCoords.x, worldCoords.y);

        for (Vector2 pos : markers.customMarkerPositions)
            if (clickPos.dst(pos) < CLICK_RADIUS) return true;

        for (Vector2 pos : markers.markerPositions)
            if (clickPos.dst(pos) < CLICK_RADIUS) return true;

        return false;
    }

    // -----------------------------
    // HANDLE MARKER CLICK
    // -----------------------------
    private void checkMarkerClick(int screenX, int screenY) {
        Vector3 worldCoords = map.camera.unproject(new Vector3(screenX, screenY, 0));
        Vector2 clickPos = new Vector2(worldCoords.x, worldCoords.y);

        markers.selected = null;

        // Custom markers
        for (int i = 0; i < markers.customMarkerPositions.size; i++) {
            Vector2 pos = markers.customMarkerPositions.get(i);
            if (clickPos.dst(pos) < CLICK_RADIUS) {

                markers.selected = markers.customMarkers.get(i);

                if (ui.editMode) {
                    ui.showEditWindow = true;
                    ui.editingMarker = markers.selected;

                    // preload fields
                    ui.newMarkerName = markers.selected.name;
                    ui.newMarkerType = markers.selected.type;
                    ui.newMarkerLat = markers.selected.latitude;
                    ui.newMarkerLon = markers.selected.longitude;
                }

                return;
            }
        }

        // API markers
        for (int i = 0; i < markers.markerPositions.size; i++) {
            Vector2 pos = markers.markerPositions.get(i);
            if (clickPos.dst(pos) < CLICK_RADIUS) {
                markers.selected = markers.markers.get(i);
                return;
            }
        }
    }

    // -----------------------------
    // UI BUTTON HANDLING
    // -----------------------------
    private void handleAddWindowClick(int x, int y) {
        int h = Gdx.graphics.getHeight();

        int panelX = 50;
        int panelY = h - 370;

        int btnW = 120;
        int btnH = 30;

        int uiY = h - y;

        // Save
        if (x >= panelX + 20 && x <= panelX + 20 + btnW &&
            uiY >= panelY + 20 && uiY <= panelY + 20 + btnH) {

            ui.addMarkerRequested = true;
            ui.showAddWindow = false;
            return;
        }

        // Cancel
        if (x >= panelX + 150 && x <= panelX + 150 + btnW &&
            uiY >= panelY + 20 && uiY <= panelY + 20 + btnH) {

            ui.showAddWindow = false;
            return;
        }
    }

    private void handleEditWindowClick(int x, int y) {
        int h = Gdx.graphics.getHeight();

        int panelX = 450;
        int panelY = h - 370;

        int btnW = 120;
        int btnH = 30;

        int uiY = h - y;

        // Save
        if (x >= panelX + 20 && x <= panelX + 20 + btnW &&
            uiY >= panelY + 20 && uiY <= panelY + 20 + btnH) {

            ui.saveEditRequested = true;
            ui.showEditWindow = false;
            return;
        }

        // Delete
        if (x >= panelX + 150 && x <= panelX + 150 + btnW &&
            uiY >= panelY + 20 && uiY <= panelY + 20 + btnH) {

            ui.deleteMarkerRequested = true;
            ui.showEditWindow = false;
            return;
        }

        // Close
        if (x >= panelX + 300 && x <= panelX + 300 + btnW &&
            uiY >= panelY + 20 && uiY <= panelY + 20 + btnH) {

            ui.showEditWindow = false;
            return;
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (stage.getKeyboardFocus() instanceof TextField) {
            return false;
        }

        // Toggle edit mode
        if (keycode == Input.Keys.E) {
            ui.editMode = !ui.editMode;
            return true;
        }

        // Add marker window
        if (keycode == Input.Keys.N && ui.editMode) {
            ui.showAddWindow = true;
            return true;
        }

        return false;
    }

    // -----------------------------
    // UNUSED INPUT METHODS
    // -----------------------------
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int x, int y, int pointer, int button) { return false; }
}

