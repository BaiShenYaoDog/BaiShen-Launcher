package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.game.NativesDirectoryType;
import cn.ChengZhiYa.BaiShenLauncher.game.Renderer;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.VersionSetting;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.decorator.DecoratorPage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.stringConverter;
import static cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n.i18n;

public final class AdvancedVersionSettingPage extends StackPane implements DecoratorPage {

    private final ObjectProperty<State> stateProperty;

    private final Profile profile;
    private final String versionId;
    private final VersionSetting versionSetting;

    private final JFXTextField txtJVMArgs;
    private final JFXTextField txtGameArgs;
    private final JFXTextField txtEnvironmentVariables;
    private final JFXTextField txtMetaspace;
    private final JFXTextField txtWrapper;
    private final JFXTextField txtPreLaunchCommand;
    private final JFXTextField txtPostExitCommand;
    private final OptionToggleButton noJVMArgsPane;
    private final OptionToggleButton noGameCheckPane;
    private final OptionToggleButton noJVMCheckPane;
    private final OptionToggleButton noNativesPatchPane;
    private final OptionToggleButton useNativeGLFWPane;
    private final OptionToggleButton useNativeOpenALPane;
    private final ComponentSublist nativesDirSublist;
    private final MultiFileItem<NativesDirectoryType> nativesDirItem;
    private final MultiFileItem.FileOption<NativesDirectoryType> nativesDirCustomOption;
    private final JFXComboBox<Renderer> cboRenderer;

    public AdvancedVersionSettingPage(Profile profile, String versionId, VersionSetting versionSetting) {
        this.profile = profile;
        this.versionId = versionId;
        this.versionSetting = versionSetting;
        this.stateProperty = new SimpleObjectProperty<>(State.fromTitle(
                versionId == null ? I18n.i18n("settings.advanced") : I18n.i18n("settings.advanced.title", versionId)
        ));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        getChildren().setAll(scrollPane);

        VBox rootPane = new VBox();
        rootPane.setFillWidth(true);
        scrollPane.setContent(rootPane);
        FXUtils.smoothScrolling(scrollPane);
        rootPane.getStyleClass().add("card-list");

        ComponentList customCommandsPane = new ComponentList();
        {
            GridPane pane = new GridPane();
            pane.setHgap(16);
            pane.setVgap(8);
            pane.getColumnConstraints().setAll(new ColumnConstraints(), FXUtils.getColumnHgrowing());

            txtGameArgs = new JFXTextField();
            txtGameArgs.setPromptText(I18n.i18n("settings.advanced.minecraft_arguments.prompt"));
            txtGameArgs.getStyleClass().add("fit-width");
            pane.addRow(0, new Label(I18n.i18n("settings.advanced.minecraft_arguments")), txtGameArgs);

            txtPreLaunchCommand = new JFXTextField();
            txtPreLaunchCommand.setPromptText(I18n.i18n("settings.advanced.precall_command.prompt"));
            txtPreLaunchCommand.getStyleClass().add("fit-width");
            pane.addRow(1, new Label(I18n.i18n("settings.advanced.precall_command")), txtPreLaunchCommand);

            txtWrapper = new JFXTextField();
            txtWrapper.setPromptText(I18n.i18n("settings.advanced.wrapper_launcher.prompt"));
            txtWrapper.getStyleClass().add("fit-width");
            pane.addRow(2, new Label(I18n.i18n("settings.advanced.wrapper_launcher")), txtWrapper);

            txtPostExitCommand = new JFXTextField();
            txtPostExitCommand.setPromptText(I18n.i18n("settings.advanced.post_exit_command.prompt"));
            txtPostExitCommand.getStyleClass().add("fit-width");
            pane.addRow(3, new Label(I18n.i18n("settings.advanced.post_exit_command")), txtPostExitCommand);

            HintPane hintPane = new HintPane();
            hintPane.setText(I18n.i18n("settings.advanced.custom_commands.hint"));
            GridPane.setColumnSpan(hintPane, 2);
            pane.addRow(4, hintPane);

            customCommandsPane.getContent().setAll(pane);
        }

        ComponentList jvmPane = new ComponentList();
        {
            GridPane pane = new GridPane();
            ColumnConstraints title = new ColumnConstraints();
            ColumnConstraints value = new ColumnConstraints();
            value.setFillWidth(true);
            value.setHgrow(Priority.ALWAYS);
            pane.setHgap(16);
            pane.setVgap(8);
            pane.getColumnConstraints().setAll(title, value);

            txtJVMArgs = new JFXTextField();
            txtJVMArgs.getStyleClass().add("fit-width");
            pane.addRow(0, new Label(I18n.i18n("settings.advanced.jvm_args")), txtJVMArgs);

            HintPane hintPane = new HintPane();
            hintPane.setText(I18n.i18n("settings.advanced.jvm_args.prompt"));
            GridPane.setColumnSpan(hintPane, 2);
            pane.addRow(4, hintPane);

            txtMetaspace = new JFXTextField();
            txtMetaspace.setPromptText(I18n.i18n("settings.advanced.java_permanent_generation_space.prompt"));
            txtMetaspace.getStyleClass().add("fit-width");
            FXUtils.setValidateWhileTextChanged(txtMetaspace, true);
            txtMetaspace.setValidators(new NumberValidator(I18n.i18n("input.number"), true));
            pane.addRow(1, new Label(I18n.i18n("settings.advanced.java_permanent_generation_space")), txtMetaspace);

            txtEnvironmentVariables = new JFXTextField();
            txtEnvironmentVariables.getStyleClass().add("fit-width");
            pane.addRow(2, new Label(I18n.i18n("settings.advanced.environment_variables")), txtEnvironmentVariables);

            jvmPane.getContent().setAll(pane);
        }

        ComponentList workaroundPane = new ComponentList();

        HintPane workaroundWarning = new HintPane(MessageDialogPane.MessageType.WARNING);
        workaroundWarning.setText(I18n.i18n("settings.advanced.workaround.warning"));

        {
            nativesDirItem = new MultiFileItem<>();
            nativesDirSublist = new ComponentSublist();
            nativesDirSublist.getContent().add(nativesDirItem);
            nativesDirSublist.setTitle(I18n.i18n("settings.advanced.natives_directory"));
            nativesDirSublist.setHasSubtitle(true);
            nativesDirCustomOption = new MultiFileItem.FileOption<>(I18n.i18n("settings.advanced.natives_directory.custom"), NativesDirectoryType.CUSTOM)
                    .setChooserTitle(I18n.i18n("settings.advanced.natives_directory.choose"))
                    .setDirectory(true);
            nativesDirItem.loadChildren(Arrays.asList(
                    new MultiFileItem.Option<>(I18n.i18n("settings.advanced.natives_directory.default"), NativesDirectoryType.VERSION_FOLDER),
                    nativesDirCustomOption
            ));
            HintPane nativesDirHint = new HintPane(MessageDialogPane.MessageType.WARNING);
            nativesDirHint.setText(I18n.i18n("settings.advanced.natives_directory.hint"));
            nativesDirItem.getChildren().add(nativesDirHint);

            BorderPane rendererPane = new BorderPane();
            {
                Label label = new Label(I18n.i18n("settings.advanced.renderer"));
                rendererPane.setLeft(label);
                BorderPane.setAlignment(label, Pos.CENTER_LEFT);

                cboRenderer = new JFXComboBox<>();
                cboRenderer.getItems().setAll(Renderer.values());
                cboRenderer.setConverter(stringConverter(e -> i18n("settings.advanced.renderer." + e.name().toLowerCase(Locale.ROOT))));
                rendererPane.setRight(cboRenderer);
                BorderPane.setAlignment(cboRenderer, Pos.CENTER_RIGHT);
                FXUtils.setLimitWidth(cboRenderer, 300);
            }

            noJVMArgsPane = new OptionToggleButton();
            noJVMArgsPane.setTitle(I18n.i18n("settings.advanced.no_jvm_args"));

            noGameCheckPane = new OptionToggleButton();
            noGameCheckPane.setTitle(I18n.i18n("settings.advanced.dont_check_game_completeness"));

            noJVMCheckPane = new OptionToggleButton();
            noJVMCheckPane.setTitle(I18n.i18n("settings.advanced.dont_check_jvm_validity"));

            noNativesPatchPane = new OptionToggleButton();
            noNativesPatchPane.setTitle(I18n.i18n("settings.advanced.dont_patch_natives"));

            useNativeGLFWPane = new OptionToggleButton();
            useNativeGLFWPane.setTitle(I18n.i18n("settings.advanced.use_native_glfw"));

            useNativeOpenALPane = new OptionToggleButton();
            useNativeOpenALPane.setTitle(I18n.i18n("settings.advanced.use_native_openal"));

            workaroundPane.getContent().setAll(
                    nativesDirSublist, rendererPane,
                    noJVMArgsPane, noGameCheckPane, noJVMCheckPane, noNativesPatchPane,
                    useNativeGLFWPane, useNativeOpenALPane);
        }

        rootPane.getChildren().addAll(
                ComponentList.createComponentListTitle(I18n.i18n("settings.advanced.custom_commands")), customCommandsPane,
                ComponentList.createComponentListTitle(I18n.i18n("settings.advanced.jvm")), jvmPane,
                ComponentList.createComponentListTitle(I18n.i18n("settings.advanced.workaround")), workaroundWarning, workaroundPane
        );

        bindProperties();
    }

    void bindProperties() {
        nativesDirCustomOption.bindBidirectional(versionSetting.nativesDirProperty());
        FXUtils.bindString(txtJVMArgs, versionSetting.javaArgsProperty());
        FXUtils.bindString(txtGameArgs, versionSetting.minecraftArgsProperty());
        FXUtils.bindString(txtMetaspace, versionSetting.permSizeProperty());
        FXUtils.bindString(txtEnvironmentVariables, versionSetting.environmentVariablesProperty());
        FXUtils.bindString(txtWrapper, versionSetting.wrapperProperty());
        FXUtils.bindString(txtPreLaunchCommand, versionSetting.preLaunchCommandProperty());
        FXUtils.bindEnum(cboRenderer, versionSetting.rendererProperty());
        noGameCheckPane.selectedProperty().bindBidirectional(versionSetting.notCheckGameProperty());
        noJVMCheckPane.selectedProperty().bindBidirectional(versionSetting.notCheckJVMProperty());
        noJVMArgsPane.selectedProperty().bindBidirectional(versionSetting.noJVMArgsProperty());
        noNativesPatchPane.selectedProperty().bindBidirectional(versionSetting.notPatchNativesProperty());
        useNativeGLFWPane.selectedProperty().bindBidirectional(versionSetting.useNativeGLFWProperty());
        useNativeOpenALPane.selectedProperty().bindBidirectional(versionSetting.useNativeOpenALProperty());

        nativesDirItem.selectedDataProperty().bindBidirectional(versionSetting.nativesDirTypeProperty());
        nativesDirSublist.subtitleProperty().bind(Bindings.createStringBinding(() -> Paths.get(profile.getRepository().getRunDirectory(versionId).getAbsolutePath() + "/natives").normalize().toString(),
                versionSetting.nativesDirProperty(), versionSetting.nativesDirTypeProperty()));
    }

    void unbindProperties() {
        nativesDirCustomOption.valueProperty().unbindBidirectional(versionSetting.nativesDirProperty());
        FXUtils.unbind(txtJVMArgs, versionSetting.javaArgsProperty());
        FXUtils.unbind(txtGameArgs, versionSetting.minecraftArgsProperty());
        FXUtils.unbind(txtMetaspace, versionSetting.permSizeProperty());
        FXUtils.unbind(txtEnvironmentVariables, versionSetting.environmentVariablesProperty());
        FXUtils.unbind(txtWrapper, versionSetting.wrapperProperty());
        FXUtils.unbind(txtPreLaunchCommand, versionSetting.preLaunchCommandProperty());
        FXUtils.unbind(txtPostExitCommand, versionSetting.postExitCommandProperty());
        FXUtils.unbindEnum(cboRenderer);
        noGameCheckPane.selectedProperty().unbindBidirectional(versionSetting.notCheckGameProperty());
        noJVMCheckPane.selectedProperty().unbindBidirectional(versionSetting.notCheckJVMProperty());
        noJVMArgsPane.selectedProperty().unbindBidirectional(versionSetting.noJVMArgsProperty());
        noNativesPatchPane.selectedProperty().unbindBidirectional(versionSetting.notPatchNativesProperty());
        useNativeGLFWPane.selectedProperty().unbindBidirectional(versionSetting.useNativeGLFWProperty());
        useNativeOpenALPane.selectedProperty().unbindBidirectional(versionSetting.useNativeOpenALProperty());

        nativesDirItem.selectedDataProperty().unbindBidirectional(versionSetting.nativesDirTypeProperty());
        nativesDirSublist.subtitleProperty().unbind();
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return stateProperty;
    }
}
