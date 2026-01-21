package si.um.feri.rri.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.UIComponent;
import si.um.feri.rri.component.enums.MarkerType;

public class UIManager {

    private final Stage stage;
    private final Skin skin;
    private final UIComponent ui;
    private final MarkerComponent markers;

    private Label statsLabel;
    private Label infoLabel;

    private Window addWindow;
    private TextField addNameField, addTypeField, addLatField, addLonField;

    private Window editWindow;
    private TextField editNameField, editTypeField, editLatField, editLonField;

    private Window infoWindow;


    public UIManager(Stage stage, Skin skin, UIComponent ui, MarkerComponent markers) {
        this.stage = stage;
        this.skin = skin;
        this.ui = ui;
        this.markers = markers;

        createHeader();
        createFilterPanel();
        createAddWindow();
        createEditWindow();
        createInfoWindow();
    }

    private void createHeader() {
        Window header = new Window("", skin);
        header.getTitleTable().clear();
        header.setMovable(false);
        header.setSize(350, 160);
        header.setPosition(0, Gdx.graphics.getHeight() - 160);
        header.pad(10);

        header.setSize(350, 160);
        header.setPosition(0, Gdx.graphics.getHeight() - 160);

        header.pad(10);

        Label title = new Label("Childplay Maribor", skin);
        statsLabel = new Label("Loading...", skin);

        header.add(title).left().row();
        header.add(statsLabel).left().row();

        stage.addActor(header);
    }

    private void createInfoWindow() {
        infoWindow = new Window("Marker Info", skin);
        infoWindow.setSize(300, 150);
        infoWindow.setPosition(Gdx.graphics.getWidth() - 320, 20); // spodaj desno

        infoLabel = new Label("No marker selected", skin);
        infoWindow.add(infoLabel).left().top().pad(10);

        stage.addActor(infoWindow);
    }

    private void createFilterPanel() {
        Window filterWin = new Window("Filters", skin);
        filterWin.setSize(200, 180);
        filterWin.setPosition(10, 10);

        CheckBox cbKind = new CheckBox("Kindergartens", skin);
        cbKind.setChecked(true);
        cbKind.addListener(e -> {
            toggleFilter(MarkerType.KINDERGARTEN, cbKind.isChecked());
            return false;
        });

        CheckBox cbPlay = new CheckBox("Playgrounds", skin);
        cbPlay.setChecked(true);
        cbPlay.addListener(e -> {
            toggleFilter(MarkerType.PLAYGROUND, cbPlay.isChecked());
            return false;
        });

        CheckBox cbTrain = new CheckBox("Train Stops", skin);
        cbTrain.setChecked(true);
        cbTrain.addListener(e -> {
            toggleFilter(MarkerType.TRAIN, cbTrain.isChecked());
            return false;
        });

        CheckBox cbCustom = new CheckBox("Custom", skin);
        cbCustom.setChecked(true);
        cbCustom.addListener(e -> {
            toggleFilter(MarkerType.CUSTOM, cbCustom.isChecked());
            return false;
        });

        filterWin.add(cbKind).left().row();
        filterWin.add(cbPlay).left().row();
        filterWin.add(cbTrain).left().row();
        filterWin.add(cbCustom).left().row();

        stage.addActor(filterWin);
    }

    private void toggleFilter(MarkerType type, boolean enabled) {
        if (enabled) ui.activeFilters.add(type);
        else ui.activeFilters.remove(type);
    }

    private void createAddWindow() {
        addWindow = new Window("Add Marker", skin);
        addWindow.setSize(300, 250);
        addWindow.setPosition(50, Gdx.graphics.getHeight() - 300);
        addWindow.setVisible(false);

        addNameField = new TextField("", skin);
        addTypeField = new TextField("", skin);
        addLatField = new TextField("", skin);
        addLonField = new TextField("", skin);

        TextButton saveBtn = new TextButton("Save", skin);
        saveBtn.addListener(e -> {
            ui.newMarkerName = addNameField.getText();
            ui.newMarkerType = addTypeField.getText();
            ui.newMarkerLat = Double.parseDouble(addLatField.getText());
            ui.newMarkerLon = Double.parseDouble(addLonField.getText());
            ui.addMarkerRequested = true;
            addWindow.setVisible(false);
            return false;
        });

        TextButton cancelBtn = new TextButton("Cancel", skin);
        cancelBtn.addListener(e -> {
            addWindow.setVisible(false);
            return false;
        });

        addWindow.add("Name:").left();
        addWindow.add(addNameField).row();
        addWindow.add("Type:").left();
        addWindow.add(addTypeField).row();
        addWindow.add("Lat:").left();
        addWindow.add(addLatField).row();
        addWindow.add("Lon:").left();
        addWindow.add(addLonField).row();
        addWindow.add(saveBtn);
        addWindow.add(cancelBtn);

        stage.addActor(addWindow);
    }

    private void createEditWindow() {
        editWindow = new Window("Edit Marker", skin);
        editWindow.setSize(300, 250);
        editWindow.setPosition(400, Gdx.graphics.getHeight() - 300);
        editWindow.setVisible(false);

        editNameField = new TextField("", skin);
        editTypeField = new TextField("", skin);
        editLatField = new TextField("", skin);
        editLonField = new TextField("", skin);

        TextButton saveBtn = new TextButton("Save", skin);
        saveBtn.addListener(e -> {
            ui.newMarkerName = editNameField.getText();
            ui.newMarkerType = editTypeField.getText();
            ui.newMarkerLat = Double.parseDouble(editLatField.getText());
            ui.newMarkerLon = Double.parseDouble(editLonField.getText());
            ui.saveEditRequested = true;
            editWindow.setVisible(false);
            return false;
        });

        TextButton deleteBtn = new TextButton("Delete", skin);
        deleteBtn.addListener(e -> {
            ui.deleteMarkerRequested = true;
            editWindow.setVisible(false);
            return false;
        });

        editWindow.add("Name:").left();
        editWindow.add(editNameField).row();
        editWindow.add("Type:").left();
        editWindow.add(editTypeField).row();
        editWindow.add("Lat:").left();
        editWindow.add(editLatField).row();
        editWindow.add("Lon:").left();
        editWindow.add(editLonField).row();
        editWindow.add(saveBtn);
        editWindow.add(deleteBtn);

        stage.addActor(editWindow);
    }

    public void update() {
        // Update stats
        statsLabel.setText("Kindergartens: " + ui.kindergartens + "\n" +
            "Playgrounds: " + ui.playgrounds + "\n" +
            "Train Stops: " + ui.trainStops);

        // Update info panel
        if (markers.selected != null) {
            infoLabel.setText(
                markers.selected.name + "\n" +
                    "Type: " + markers.selected.type + "\n" +
                    String.format("Lat: %.4f\nLon: %.4f",
                        markers.selected.latitude,
                        markers.selected.longitude)
            );
        } else {
            infoLabel.setText("No marker selected");
        }

        // Show Add window
        if (ui.showAddWindow) {
            addNameField.setText("");
            addTypeField.setText("");
            addLatField.setText("");
            addLonField.setText("");
            addWindow.setVisible(true);
            ui.showAddWindow = false;
        }

        // Show Edit window
        if (ui.showEditWindow && ui.editingMarker != null) {
            editNameField.setText(ui.editingMarker.name);
            editTypeField.setText(ui.editingMarker.type);
            editLatField.setText(String.valueOf(ui.editingMarker.latitude));
            editLonField.setText(String.valueOf(ui.editingMarker.longitude));
            editWindow.setVisible(true);
            ui.showEditWindow = false;
        }
    }
}
