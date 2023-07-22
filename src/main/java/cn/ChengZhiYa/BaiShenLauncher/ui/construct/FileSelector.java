package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;

public class FileSelector extends HBox {
    private final StringProperty value = new SimpleStringProperty();
    private final ObservableList<FileChooser.ExtensionFilter> extensionFilters = FXCollections.observableArrayList();
    private String chooserTitle = I18n.i18n("selector.choose_file");
    private boolean directory = false;

    public FileSelector() {
        JFXTextField customField = new JFXTextField();
        FXUtils.bindString(customField, valueProperty());

        JFXButton selectButton = new JFXButton();
        selectButton.setGraphic(SVG.folderOpen(Theme.blackFillBinding(), 15, 15));
        selectButton.setOnAction(e -> {
            if (directory) {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle(chooserTitle);
                File dir = chooser.showDialog(Controllers.getStage());
                if (dir != null) {
                    String path = dir.getAbsolutePath();
                    customField.setText(path);
                    value.setValue(path);
                }
            } else {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().addAll(getExtensionFilters());
                chooser.setTitle(chooserTitle);
                File file = chooser.showOpenDialog(Controllers.getStage());
                if (file != null) {
                    String path = file.getAbsolutePath();
                    customField.setText(path);
                    value.setValue(path);
                }
            }
        });

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(3);
        getChildren().addAll(customField, selectButton);
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public StringProperty valueProperty() {
        return value;
    }

    public String getChooserTitle() {
        return chooserTitle;
    }

    public FileSelector setChooserTitle(String chooserTitle) {
        this.chooserTitle = chooserTitle;
        return this;
    }

    public boolean isDirectory() {
        return directory;
    }

    public FileSelector setDirectory(boolean directory) {
        this.directory = directory;
        return this;
    }

    public ObservableList<FileChooser.ExtensionFilter> getExtensionFilters() {
        return extensionFilters;
    }
}
