package cn.ChengZhiYa.BaiShenLauncher.ui.download;

import cn.ChengZhiYa.BaiShenLauncher.game.HMCLGameRepository;
import cn.ChengZhiYa.BaiShenLauncher.game.ManuallyCreatedModpackException;
import cn.ChengZhiYa.BaiShenLauncher.game.ModpackHelper;
import cn.ChengZhiYa.BaiShenLauncher.mod.Modpack;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.RequiredValidator;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Validator;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.CompressingUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class LocalModpackPage extends ModpackPage {

    public static final String MODPACK_FILE = "MODPACK_FILE";
    public static final String MODPACK_NAME = "MODPACK_NAME";
    public static final String MODPACK_MANIFEST = "MODPACK_MANIFEST";
    public static final String MODPACK_CHARSET = "MODPACK_CHARSET";
    public static final String MODPACK_MANUALLY_CREATED = "MODPACK_MANUALLY_CREATED";
    private final BooleanProperty installAsVersion = new SimpleBooleanProperty(true);
    private Modpack manifest = null;
    private Charset charset;
    public LocalModpackPage(WizardController controller) {
        super(controller);

        Profile profile = (Profile) controller.getSettings().get("PROFILE");

        Optional<String> name = Lang.tryCast(controller.getSettings().get(MODPACK_NAME), String.class);
        if (name.isPresent()) {
            txtModpackName.setText(name.get());
            txtModpackName.setDisable(true);
        } else {
            FXUtils.onChangeAndOperate(installAsVersion, installAsVersion -> {
                if (installAsVersion) {
                    txtModpackName.getValidators().setAll(
                            new RequiredValidator(),
                            new Validator(I18n.i18n("install.new_game.already_exists"), str -> !profile.getRepository().versionIdConflicts(str)),
                            new Validator(I18n.i18n("install.new_game.malformed"), HMCLGameRepository::isValidVersionId));
                } else {
                    txtModpackName.getValidators().setAll(
                            new RequiredValidator(),
                            new Validator(I18n.i18n("install.new_game.already_exists"), str -> !ModpackHelper.isExternalGameNameConflicts(str) && Profiles.getProfiles().stream().noneMatch(p -> p.getName().equals(str))),
                            new Validator(I18n.i18n("install.new_game.malformed"), HMCLGameRepository::isValidVersionId));
                }
            });
        }

        File selectedFile;
        Optional<File> filePath = Lang.tryCast(controller.getSettings().get(MODPACK_FILE), File.class);
        if (filePath.isPresent()) {
            selectedFile = filePath.get();
        } else {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(I18n.i18n("modpack.choose"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("modpack"), "*.zip"));
            selectedFile = chooser.showOpenDialog(Controllers.getStage());
            if (selectedFile == null) {
                controller.onEnd();
                return;
            }

            controller.getSettings().put(MODPACK_FILE, selectedFile);
        }

        showSpinner();
        Task.supplyAsync(() -> CompressingUtils.findSuitableEncoding(selectedFile.toPath()))
                .thenApplyAsync(encoding -> {
                    charset = encoding;
                    manifest = ModpackHelper.readModpackManifest(selectedFile.toPath(), encoding);
                    return manifest;
                })
                .whenComplete(Schedulers.javafx(), (manifest, exception) -> {
                    if (exception instanceof ManuallyCreatedModpackException) {
                        hideSpinner();
                        lblName.setText(selectedFile.getName());
                        installAsVersion.set(false);
                        lblModpackLocation.setText(selectedFile.getAbsolutePath());

                        if (!name.isPresent()) {
                            // trim: https://github.com/huanghongxun/HMCL/issues/962
                            txtModpackName.setText(FileUtils.getNameWithoutExtension(selectedFile));
                        }

                        Controllers.confirm(I18n.i18n("modpack.type.manual.warning"), I18n.i18n("install.modpack"), MessageDialogPane.MessageType.WARNING,
                                () -> {
                                },
                                controller::onEnd);

                        controller.getSettings().put(MODPACK_MANUALLY_CREATED, true);
                    } else if (exception != null) {
                        Logging.LOG.log(Level.WARNING, "Failed to read modpack manifest", exception);
                        Controllers.dialog(I18n.i18n("modpack.task.install.error"), I18n.i18n("message.error"), MessageDialogPane.MessageType.ERROR);
                        Platform.runLater(controller::onEnd);
                    } else {
                        hideSpinner();
                        controller.getSettings().put(MODPACK_MANIFEST, manifest);
                        lblName.setText(manifest.getName());
                        lblVersion.setText(manifest.getVersion());
                        lblAuthor.setText(manifest.getAuthor());

                        lblModpackLocation.setText(selectedFile.getAbsolutePath());

                        if (!name.isPresent()) {
                            // trim: https://github.com/huanghongxun/HMCL/issues/962
                            txtModpackName.setText(manifest.getName().trim());
                        }
                    }
                }).start();
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
        settings.remove(MODPACK_FILE);
    }

    protected void onInstall() {
        if (!txtModpackName.validate()) return;
        controller.getSettings().put(MODPACK_NAME, txtModpackName.getText());
        controller.getSettings().put(MODPACK_CHARSET, charset);
        controller.onFinish();
    }

    protected void onDescribe() {
        if (manifest != null) {
            FXUtils.showWebDialog(I18n.i18n("modpack.description"), manifest.getDescription());
        }
    }
}
