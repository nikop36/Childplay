package si.um.feri.rri.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import si.um.feri.rri.component.MarkerComponent;
import si.um.feri.rri.component.UIComponent;
import si.um.feri.rri.component.enums.MarkerType;

public class UIManager {

    private final Stage stage;
    private final Skin skin;
    private final UIComponent ui;
    private final MarkerComponent markers;

    private Label editModeLabel;
    private Label statsLabel;
    private Label infoLabel;

    private Window addWindow;
    private TextField addNameField, addTypeField;

    private Window editWindow;
    private TextField editNameField, editTypeField;

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
        header.setPosition(10, Gdx.graphics.getHeight() - header.getHeight() - 10);
        header.pad(10);

        Label title = new Label("Childplay Maribor", skin);
        statsLabel = new Label("Loading...", skin);
        editModeLabel = new Label("Mode: VIEW (press E)", skin);

        header.add(title).left().row();
        header.add(statsLabel).left().row();
        header.add(editModeLabel).left().row();

        stage.addActor(header);
    }

    private void createInfoWindow() {
        infoWindow = new Window("Marker Info", skin);
        infoWindow.setSize(300, 150);
        infoWindow.setPosition( Gdx.graphics.getWidth() - infoWindow.getWidth() - 10, 10 );

        infoLabel = new Label("No marker selected", skin);
        infoLabel.setWrap(true);
        infoWindow.row();
        infoWindow.add(infoLabel).width(280).left().top().pad(10);


        stage.addActor(infoWindow);
    }

    private void createFilterPanel() {
        Window filterWin = new Window("Filters", skin);
        filterWin.setSize(200, 180);
        filterWin.setPosition(10, 10);

        CheckBox cbKind = new CheckBox("Kindergartens", skin);
        cbKind.setChecked(true);
        cbKind.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleFilter(MarkerType.KINDERGARTEN, cbKind.isChecked());
            }
        });

        CheckBox cbPlay = new CheckBox("Playgrounds", skin);
        cbPlay.setChecked(true);
        cbPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleFilter(MarkerType.PLAYGROUND, cbPlay.isChecked());
            }
        });

        CheckBox cbTrain = new CheckBox("Train Stops", skin);
        cbTrain.setChecked(true);
        cbTrain.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleFilter(MarkerType.TRAIN, cbTrain.isChecked());
            }
        });

        CheckBox cbCustom = new CheckBox("Custom", skin);
        cbCustom.setChecked(true);
        cbCustom.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                toggleFilter(MarkerType.CUSTOM, cbCustom.isChecked());
            }
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
        addWindow.setSize(300, 180);
        addWindow.setPosition(
            Gdx.graphics.getWidth() / 2f - addWindow.getWidth() / 2f,
            Gdx.graphics.getHeight() / 2f - addWindow.getHeight() / 2f
        );
        addWindow.setVisible(false);

        addNameField = new TextField("", skin);
        addTypeField = new TextField("", skin);

        TextButton saveBtn = new TextButton("Save", skin);
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                ui.pendingMarkerName = addNameField.getText();
                ui.pendingMarkerType = addTypeField.getText();

                ui.waitingForPlacement = true;

                addWindow.setVisible(false);
                stage.unfocusAll();
            }
        });

        TextButton cancelBtn = new TextButton("Cancel", skin);
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                addWindow.setVisible(false);
                stage.unfocusAll();
            }
        });

        addWindow.add("Name:").left();
        addWindow.add(addNameField).row();
        addWindow.add("Type:").left();
        addWindow.add(addTypeField).row();
        addWindow.add(saveBtn);
        addWindow.add(cancelBtn);

        stage.addActor(addWindow);
    }

    private void createEditWindow() {
        editWindow = new Window("Edit Marker", skin);
        editWindow.setSize(300, 180);
        editWindow.setPosition(400, Gdx.graphics.getHeight() - 300);
        editWindow.setVisible(false);

        editNameField = new TextField("", skin);
        editTypeField = new TextField("", skin);

        TextButton saveBtn = new TextButton("Save", skin);
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ui.newMarkerName = editNameField.getText();
                ui.newMarkerType = editTypeField.getText();
                ui.saveEditRequested = true;

                editWindow.setVisible(false);
                stage.unfocusAll();
            }
        });

        TextButton deleteBtn = new TextButton("Delete", skin);
        deleteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ui.deleteMarkerRequested = true;

                editWindow.setVisible(false);
                stage.unfocusAll();
            }
        });

        editWindow.add("Name:").left();
        editWindow.add(editNameField).row();
        editWindow.add("Type:").left();
        editWindow.add(editTypeField).row();
        editWindow.add(saveBtn);
        editWindow.add(deleteBtn);

        stage.addActor(editWindow);
    }

    public void update() {
        statsLabel.setText("Kindergartens: " + ui.kindergartens + "\n" +
            "Playgrounds: " + ui.playgrounds + "\n" +
            "Train Stops: " + ui.trainStops);

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

        if (ui.showAddWindow) {
            addNameField.setText("");
            addTypeField.setText("");
            addWindow.setVisible(true);
            stage.setKeyboardFocus(addNameField);
            ui.showAddWindow = false;
        }

        if (ui.showEditWindow && ui.editingMarker != null) {
            editNameField.setText(ui.editingMarker.name);
            editTypeField.setText(ui.editingMarker.type);
            editWindow.setVisible(true);
            stage.setKeyboardFocus(editNameField);
            ui.showEditWindow = false;
        }

        if (ui.waitingForPlacement) {
            infoLabel.setText("IN PLACEMENT MODE...\nClick on map to place marker");
        }

        editModeLabel.setText(ui.editMode ? "Mode: EDIT (press E)" : "Mode: VIEW (press E)");
    }
}
