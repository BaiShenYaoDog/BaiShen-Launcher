package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.mod.LocalModFile;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModManager;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteMod;
import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.PageCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;

public class ModUpdatesPage extends BorderPane implements DecoratorPage {
    private final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(DecoratorPage.State.fromTitle(I18n.i18n("mods.check_updates")));

    private final ModManager modManager;
    private final ObservableList<ModUpdateObject> objects;

    @SuppressWarnings("unchecked")
    public ModUpdatesPage(ModManager modManager, List<LocalModFile.ModUpdate> updates) {
        this.modManager = modManager;

        getStyleClass().add("gray-background");

        TableColumn<ModUpdateObject, Boolean> enabledColumn = new TableColumn<>();
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        setupCellValueFactory(enabledColumn, ModUpdateObject::enabledProperty);
        enabledColumn.setEditable(true);
        enabledColumn.setMaxWidth(40);
        enabledColumn.setMinWidth(40);

        TableColumn<ModUpdateObject, String> fileNameColumn = new TableColumn<>(I18n.i18n("mods.check_updates.file"));
        fileNameColumn.setPrefWidth(200);
        setupCellValueFactory(fileNameColumn, ModUpdateObject::fileNameProperty);

        TableColumn<ModUpdateObject, String> currentVersionColumn = new TableColumn<>(I18n.i18n("mods.check_updates.current_version"));
        currentVersionColumn.setPrefWidth(200);
        setupCellValueFactory(currentVersionColumn, ModUpdateObject::currentVersionProperty);

        TableColumn<ModUpdateObject, String> targetVersionColumn = new TableColumn<>(I18n.i18n("mods.check_updates.target_version"));
        targetVersionColumn.setPrefWidth(200);
        setupCellValueFactory(targetVersionColumn, ModUpdateObject::targetVersionProperty);

        TableColumn<ModUpdateObject, String> sourceColumn = new TableColumn<>(I18n.i18n("mods.check_updates.source"));
        setupCellValueFactory(sourceColumn, ModUpdateObject::sourceProperty);

        objects = FXCollections.observableList(updates.stream().map(ModUpdateObject::new).collect(Collectors.toList()));

        TableView<ModUpdateObject> table = new TableView<>(objects);
        table.setEditable(true);
        table.getColumns().setAll(enabledColumn, fileNameColumn, currentVersionColumn, targetVersionColumn, sourceColumn);

        setCenter(table);

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(8));
        actions.setAlignment(Pos.CENTER_RIGHT);

        JFXButton nextButton = FXUtils.newRaisedButton(I18n.i18n("mods.check_updates.update"));
        nextButton.setOnAction(e -> updateMods());

        JFXButton cancelButton = FXUtils.newRaisedButton(I18n.i18n("button.cancel"));
        cancelButton.setOnAction(e -> fireEvent(new PageCloseEvent()));
        onEscPressed(this, cancelButton::fire);

        actions.getChildren().setAll(nextButton, cancelButton);
        setBottom(actions);
    }

    private <T> void setupCellValueFactory(TableColumn<ModUpdateObject, T> column, Function<ModUpdateObject, ObservableValue<T>> mapper) {
        column.setCellValueFactory(param -> mapper.apply(param.getValue()));
    }

    private void updateMods() {
        ModUpdateTask task = new ModUpdateTask(
                modManager,
                objects.stream()
                        .filter(o -> o.enabled.get())
                        .map(object -> Pair.pair(object.data.getLocalMod(), object.data.getCandidates().get(0)))
                        .collect(Collectors.toList()));
        Controllers.taskDialog(
                task.whenComplete(Schedulers.javafx(), exception -> {
                    fireEvent(new PageCloseEvent());
                    if (!task.getFailedMods().isEmpty()) {
                        Controllers.dialog(I18n.i18n("mods.check_updates.failed") + "\n" +
                                        task.getFailedMods().stream().map(LocalModFile::getFileName).collect(Collectors.joining("\n")),
                                I18n.i18n("install.failed"),
                                MessageDialogPane.MessageType.ERROR);
                    }

                    if (exception == null) {
                        Controllers.dialog(I18n.i18n("install.success"));
                    }
                }),
                I18n.i18n("mods.check_updates.update"),
                TaskCancellationAction.NORMAL);
    }

    @Override
    public ReadOnlyObjectWrapper<State> stateProperty() {
        return state;
    }

    private static final class ModUpdateObject {
        final LocalModFile.ModUpdate data;
        final BooleanProperty enabled = new SimpleBooleanProperty();
        final StringProperty fileName = new SimpleStringProperty();
        final StringProperty currentVersion = new SimpleStringProperty();
        final StringProperty targetVersion = new SimpleStringProperty();
        final StringProperty source = new SimpleStringProperty();

        public ModUpdateObject(LocalModFile.ModUpdate data) {
            this.data = data;

            enabled.set(!data.getLocalMod().getModManager().isDisabled(data.getLocalMod().getFile()));
            fileName.set(data.getLocalMod().getFileName());
            currentVersion.set(data.getCurrentVersion().getVersion());
            targetVersion.set(data.getCandidates().get(0).getVersion());
            switch (data.getCurrentVersion().getSelf().getType()) {
                case CURSEFORGE:
                    source.set(I18n.i18n("mods.curseforge"));
                    break;
                case MODRINTH:
                    source.set(I18n.i18n("mods.modrinth"));
            }
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }

        public String getFileName() {
            return fileName.get();
        }

        public void setFileName(String fileName) {
            this.fileName.set(fileName);
        }

        public StringProperty fileNameProperty() {
            return fileName;
        }

        public String getCurrentVersion() {
            return currentVersion.get();
        }

        public void setCurrentVersion(String currentVersion) {
            this.currentVersion.set(currentVersion);
        }

        public StringProperty currentVersionProperty() {
            return currentVersion;
        }

        public String getTargetVersion() {
            return targetVersion.get();
        }

        public void setTargetVersion(String targetVersion) {
            this.targetVersion.set(targetVersion);
        }

        public StringProperty targetVersionProperty() {
            return targetVersion;
        }

        public String getSource() {
            return source.get();
        }

        public void setSource(String source) {
            this.source.set(source);
        }

        public StringProperty sourceProperty() {
            return source;
        }
    }

    public static class ModUpdateTask extends Task<Void> {
        private final Collection<Task<?>> dependents;
        private final List<LocalModFile> failedMods = new ArrayList<>();

        ModUpdateTask(ModManager modManager, List<Pair<LocalModFile, RemoteMod.Version>> mods) {
            setStage("mods.check_updates.update");
            getProperties().put("total", mods.size());

            this.dependents = new ArrayList<>();
            for (Pair<LocalModFile, RemoteMod.Version> mod : mods) {
                LocalModFile local = mod.getKey();
                RemoteMod.Version remote = mod.getValue();
                boolean isDisabled = local.getModManager().isDisabled(local.getFile());

                dependents.add(Task
                        .runAsync(Schedulers.javafx(), () -> local.setOld(true))
                        .thenComposeAsync(() -> {
                            String fileName = remote.getFile().getFilename();
                            if (isDisabled)
                                fileName += ModManager.DISABLED_EXTENSION;

                            FileDownloadTask task = new FileDownloadTask(
                                    new URL(remote.getFile().getUrl()),
                                    modManager.getModsDirectory().resolve(fileName).toFile());

                            task.setName(remote.getName());
                            return task;
                        })
                        .whenComplete(Schedulers.javafx(), exception -> {
                            if (exception != null) {
                                // restore state if failed
                                local.setOld(false);
                                if (isDisabled)
                                    local.disable();
                                failedMods.add(local);
                            }
                        })
                        .withCounter("mods.check_updates.update"));
            }
        }

        public List<LocalModFile> getFailedMods() {
            return failedMods;
        }

        @Override
        public Collection<Task<?>> getDependents() {
            return dependents;
        }

        @Override
        public boolean doPreExecute() {
            return true;
        }

        @Override
        public void preExecute() {
            notifyPropertiesChanged();
        }

        @Override
        public boolean isRelyingOnDependents() {
            return false;
        }

        @Override
        public void execute() throws Exception {
            if (!isDependentsSucceeded())
                throw getException();
        }
    }
}
