package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.game.ModpackHelper;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackManifest;
import cn.ChengZhiYa.BaiShenLauncher.task.FileDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.task.GetTask;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardPage;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.TaskCancellationAction;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static cn.ChengZhiYa.BaiShenLauncher.ui.download.LocalModpackPage.MODPACK_FILE;

public final class ModpackSelectionPage extends VBox implements WizardPage {
    private final WizardController controller;

    public ModpackSelectionPage(WizardController controller) {
        this.controller = controller;

        Label title = new Label(I18n.i18n("install.modpack"));
        title.setPadding(new Insets(8));

        this.getStyleClass().add("jfx-list-view");
        this.setMaxSize(400, 150);
        this.setSpacing(8);
        this.getChildren().setAll(
                title,
                createButton("local", this::onChooseLocalFile),
                createButton("remote", this::onChooseRemoteFile)
        );

        Optional<File> filePath = Lang.tryCast(controller.getSettings().get(MODPACK_FILE), File.class);
        if (filePath.isPresent()) {
            controller.getSettings().put(MODPACK_FILE, filePath.get());
            Platform.runLater(controller::onNext);
        }

        FXUtils.applyDragListener(this, ModpackHelper::isFileModpackByExtension, modpacks -> {
            File modpack = modpacks.get(0);
            controller.getSettings().put(MODPACK_FILE, modpack);
            controller.onNext();
        });
    }

    private JFXButton createButton(String type, Runnable action) {
        JFXButton button = new JFXButton();

        button.getStyleClass().add("card");
        button.setStyle("-fx-cursor: HAND;");
        button.prefWidthProperty().bind(this.widthProperty());
        button.setOnAction(e -> action.run());

        BorderPane graphic = new BorderPane();
        graphic.setMouseTransparent(true);
        graphic.setLeft(new TwoLineListItem(I18n.i18n("modpack.choose." + type), I18n.i18n("modpack.choose." + type + ".detail")));

        SVGPath arrow = new SVGPath();
        arrow.setContent(SVG.ARROW_RIGHT);
        BorderPane.setAlignment(arrow, Pos.CENTER);
        graphic.setRight(arrow);

        button.setGraphic(graphic);

        JFXDepthManager.setDepth(button, 1);

        return button;
    }

    private void onChooseLocalFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.i18n("modpack.choose"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("modpack"), "*.zip", "*.mrpack"));
        File selectedFile = chooser.showOpenDialog(Controllers.getStage());
        if (selectedFile == null) {
            Platform.runLater(controller::onEnd);
            return;
        }

        controller.getSettings().put(MODPACK_FILE, selectedFile);
        controller.onNext();
    }

    private void onChooseRemoteFile() {
        Controllers.prompt(I18n.i18n("modpack.choose.remote.tooltip"), (urlString, resolve, reject) -> {
            try {
                URL url = new URL(urlString);
                if (urlString.endsWith("server-manifest.json")) {
                    // if urlString ends with .json, we assume that the url is server-manifest.json
                    Controllers.taskDialog(new GetTask(url).whenComplete(Schedulers.javafx(), (result, e) -> {
                        ServerModpackManifest manifest = JsonUtils.fromMaybeMalformedJson(result, ServerModpackManifest.class);
                        if (manifest == null) {
                            reject.accept(I18n.i18n("modpack.type.server.malformed"));
                        } else if (e == null) {
                            resolve.run();
                            controller.getSettings().put(RemoteModpackPage.MODPACK_SERVER_MANIFEST, manifest);
                            controller.onNext();
                        } else {
                            reject.accept(e.getMessage());
                        }
                    }).executor(true), I18n.i18n("message.downloading"), TaskCancellationAction.NORMAL);
                } else {
                    // otherwise we still consider the file as modpack zip file
                    // since casually the url may not ends with ".zip"
                    Path modpack = Files.createTempFile("modpack", ".zip");
                    resolve.run();

                    Controllers.taskDialog(
                            new FileDownloadTask(url, modpack.toFile(), null)
                                    .whenComplete(Schedulers.javafx(), e -> {
                                        if (e == null) {
                                            resolve.run();
                                            controller.getSettings().put(MODPACK_FILE, modpack.toFile());
                                            controller.onNext();
                                        } else {
                                            reject.accept(e.getMessage());
                                        }
                                    }).executor(true),
                            I18n.i18n("message.downloading"),
                            TaskCancellationAction.NORMAL
                    );
                }
            } catch (IOException e) {
                reject.accept(e.getMessage());
            }
        });
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
    }

    @Override
    public String getTitle() {
        return I18n.i18n("modpack.task.install");
    }
}
