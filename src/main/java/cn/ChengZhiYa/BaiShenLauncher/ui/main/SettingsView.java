package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.EnumCommonDirectory;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentList;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentSublist;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MultiFileItem;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.Locales;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.stringConverter;

public abstract class SettingsView extends StackPane {
    protected final JFXComboBox<Locales.SupportedLocale> cboLanguage;
    protected final MultiFileItem<EnumCommonDirectory> fileCommonLocation;
    protected final ComponentSublist fileCommonLocationSublist;
    protected final ScrollPane scroll;

    public SettingsView() {
        scroll = new ScrollPane();
        getChildren().setAll(scroll);
        scroll.setStyle("-fx-font-size: 14;");
        scroll.setFitToWidth(true);

        {
            VBox rootPane = new VBox();
            rootPane.setPadding(new Insets(32, 10, 32, 10));
            {
                ComponentList settingsPane = new ComponentList();
                {
                    fileCommonLocation = new MultiFileItem<>();
                    fileCommonLocationSublist = new ComponentSublist();
                    fileCommonLocationSublist.getContent().add(fileCommonLocation);
                    fileCommonLocationSublist.setTitle(I18n.i18n("launcher.cache_directory"));
                    fileCommonLocationSublist.setHasSubtitle(true);
                    fileCommonLocation.loadChildren(Arrays.asList(
                            new MultiFileItem.Option<>(I18n.i18n("launcher.cache_directory.default"), EnumCommonDirectory.DEFAULT),
                            new MultiFileItem.FileOption<>(I18n.i18n("settings.custom"), EnumCommonDirectory.CUSTOM)
                                    .setChooserTitle(I18n.i18n("launcher.cache_directory.choose"))
                                    .setDirectory(true)
                                    .bindBidirectional(ConfigHolder.config().commonDirectoryProperty())
                    ));

                    {
                        JFXButton cleanButton = new JFXButton(I18n.i18n("launcher.cache_directory.clean"));
                        cleanButton.setOnMouseClicked(e -> clearCacheDirectory());
                        cleanButton.getStyleClass().add("jfx-button-border");

                        fileCommonLocationSublist.setHeaderRight(cleanButton);
                    }

                    settingsPane.getContent().add(fileCommonLocationSublist);
                }

                {
                    BorderPane languagePane = new BorderPane();

                    Label left = new Label(I18n.i18n("settings.launcher.language"));
                    BorderPane.setAlignment(left, Pos.CENTER_LEFT);
                    languagePane.setLeft(left);

                    cboLanguage = new JFXComboBox<>();
                    cboLanguage.setConverter(stringConverter(locale -> locale.getName(ConfigHolder.config().getLocalization().getResourceBundle())));
                    FXUtils.setLimitWidth(cboLanguage, 400);
                    languagePane.setRight(cboLanguage);

                    settingsPane.getContent().add(languagePane);
                }

                {
                    BorderPane debugPane = new BorderPane();

                    Label left = new Label(I18n.i18n("settings.launcher.debug"));
                    BorderPane.setAlignment(left, Pos.CENTER_LEFT);
                    debugPane.setLeft(left);

                    JFXButton logButton = new JFXButton(I18n.i18n("settings.launcher.launcher_log.export"));
                    logButton.setOnMouseClicked(e -> onExportLogs());
                    logButton.getStyleClass().add("jfx-button-border");
                    debugPane.setRight(logButton);

                    settingsPane.getContent().add(debugPane);
                }

                rootPane.getChildren().add(settingsPane);
            }
            scroll.setContent(rootPane);
        }
    }

    protected abstract void onUpdate();

    protected abstract void onExportLogs();

    protected abstract void onSponsor();

    protected abstract void clearCacheDirectory();
}
