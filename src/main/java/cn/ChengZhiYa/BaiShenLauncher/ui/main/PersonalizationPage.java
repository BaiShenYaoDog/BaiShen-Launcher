package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.EnumBackgroundImage;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.*;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.SafeStringConverter;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.When;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Arrays;

public class PersonalizationPage extends StackPane {

    public PersonalizationPage() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(content);
        FXUtils.smoothScrolling(scrollPane);
        scrollPane.setFitToWidth(true);
        getChildren().setAll(scrollPane);

        ComponentList themeList = new ComponentList();
        {
            BorderPane themePane = new BorderPane();
            themeList.getContent().add(themePane);

            Label left = new Label(I18n.i18n("settings.launcher.theme"));
            BorderPane.setAlignment(left, Pos.CENTER_LEFT);
            themePane.setLeft(left);

            StackPane themeColorPickerContainer = new StackPane();
            themeColorPickerContainer.setMinHeight(30);
            themePane.setRight(themeColorPickerContainer);

            ColorPicker picker = new ColorPicker(Color.web(Theme.getTheme().getColor()));
            picker.getCustomColors().setAll(Theme.SUGGESTED_COLORS);
            picker.setOnAction(e -> {
                Theme theme = Theme.custom(Theme.getColorDisplayName(picker.getValue()));
                ConfigHolder.config().setTheme(theme);
                Controllers.getScene().getStylesheets().setAll(theme.getStylesheets(ConfigHolder.config().getLauncherFontFamily()));
            });
            themeColorPickerContainer.getChildren().setAll(picker);
            Platform.runLater(() -> JFXDepthManager.setDepth(picker, 0));
        }
        {
            OptionToggleButton animationButton = new OptionToggleButton();
            themeList.getContent().add(animationButton);
            animationButton.selectedProperty().bindBidirectional(ConfigHolder.config().animationDisabledProperty());
            animationButton.setTitle(I18n.i18n("settings.launcher.turn_off_animations"));
        }
        content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("settings.launcher.appearance")), themeList);

        {
            ComponentList componentList = new ComponentList();

            MultiFileItem<EnumBackgroundImage> backgroundItem = new MultiFileItem<>();
            ComponentSublist backgroundSublist = new ComponentSublist();
            backgroundSublist.getContent().add(backgroundItem);
            backgroundSublist.setTitle(I18n.i18n("launcher.background"));
            backgroundSublist.setHasSubtitle(true);

            backgroundItem.loadChildren(Arrays.asList(
                    new MultiFileItem.Option<>("JK装-OP", EnumBackgroundImage.JKOP),
                    new MultiFileItem.Option<>("JK装-ED", EnumBackgroundImage.JKED),
                    new MultiFileItem.Option<>("泳装-OP", EnumBackgroundImage.YZOP),
                    new MultiFileItem.Option<>("泳装-ED", EnumBackgroundImage.YZED),
                    new MultiFileItem.Option<>("小洋装-ED", EnumBackgroundImage.XYZED),
                    new MultiFileItem.FileOption<>(I18n.i18n("settings.custom"), EnumBackgroundImage.CUSTOM)
                            .setChooserTitle(I18n.i18n("launcher.background.choose"))
                            .bindBidirectional(ConfigHolder.config().backgroundImageProperty()),
                    new MultiFileItem.StringOption<>(I18n.i18n("launcher.background.network"), EnumBackgroundImage.NETWORK)
                            .setValidators(new URLValidator(true))
                            .bindBidirectional(ConfigHolder.config().backgroundImageUrlProperty())
            ));
            backgroundItem.selectedDataProperty().bindBidirectional(ConfigHolder.config().backgroundImageTypeProperty());
            backgroundSublist.subtitleProperty().bind(
                    new When(backgroundItem.selectedDataProperty().isEqualTo(EnumBackgroundImage.JKOP))
                            .then(I18n.i18n("launcher.background.default"))
                            .otherwise(ConfigHolder.config().backgroundImageProperty()));

            componentList.getContent().add(backgroundItem);
            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("launcher.background")), componentList);
        }

        {
            ComponentList logPane = new ComponentSublist();
            logPane.setTitle(I18n.i18n("settings.launcher.log"));

            {
                VBox fontPane = new VBox();
                fontPane.setSpacing(5);

                {
                    BorderPane borderPane = new BorderPane();
                    fontPane.getChildren().add(borderPane);
                    {
                        Label left = new Label(I18n.i18n("settings.launcher.log.font"));
                        BorderPane.setAlignment(left, Pos.CENTER_LEFT);
                        borderPane.setLeft(left);
                    }

                    {
                        HBox hBox = new HBox();
                        hBox.setSpacing(3);

                        FontComboBox cboLogFont = new FontComboBox();
                        cboLogFont.valueProperty().bindBidirectional(ConfigHolder.config().fontFamilyProperty());

                        JFXTextField txtLogFontSize = new JFXTextField();
                        FXUtils.setLimitWidth(txtLogFontSize, 50);
                        FXUtils.bind(txtLogFontSize, ConfigHolder.config().fontSizeProperty(), SafeStringConverter.fromFiniteDouble()
                                .restrict(it -> it > 0)
                                .fallbackTo(12.0)
                                .asPredicate(Validator.addTo(txtLogFontSize)));

                        hBox.getChildren().setAll(cboLogFont, txtLogFontSize);

                        borderPane.setRight(hBox);
                    }
                }

                Label lblLogFontDisplay = new Label("[23:33:33] [Client Thread/INFO] [WaterPower]: Loaded mod WaterPower.");
                lblLogFontDisplay.fontProperty().bind(Bindings.createObjectBinding(
                        () -> Font.font(Lang.requireNonNullElse(ConfigHolder.config().getFontFamily(), FXUtils.DEFAULT_MONOSPACE_FONT), ConfigHolder.config().getFontSize()),
                        ConfigHolder.config().fontFamilyProperty(), ConfigHolder.config().fontSizeProperty()));

                fontPane.getChildren().add(lblLogFontDisplay);

                logPane.getContent().add(fontPane);
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("settings.launcher.log")), logPane);
        }

        {
            ComponentSublist fontPane = new ComponentSublist();
            fontPane.setTitle(I18n.i18n("settings.launcher.font"));

            {
                VBox vbox = new VBox();
                vbox.setSpacing(5);

                {
                    BorderPane borderPane = new BorderPane();
                    vbox.getChildren().add(borderPane);
                    {
                        Label left = new Label(I18n.i18n("settings.launcher.font"));
                        BorderPane.setAlignment(left, Pos.CENTER_LEFT);
                        borderPane.setLeft(left);
                    }

                    {
                        HBox hBox = new HBox();
                        hBox.setSpacing(8);

                        FontComboBox cboFont = new FontComboBox();
                        cboFont.valueProperty().bindBidirectional(ConfigHolder.config().launcherFontFamilyProperty());

                        JFXButton clearButton = new JFXButton();
                        clearButton.getStyleClass().add("toggle-icon4");
                        clearButton.setGraphic(SVG.restore(Theme.blackFillBinding(), -1, -1));
                        clearButton.setOnAction(e -> ConfigHolder.config().setLauncherFontFamily(null));

                        hBox.getChildren().setAll(cboFont, clearButton);

                        borderPane.setRight(hBox);
                    }
                }

                Label lblFontDisplay = new Label("白神启动器 | 欢迎您的使用");
                lblFontDisplay.fontProperty().bind(Bindings.createObjectBinding(
                        () -> Font.font(ConfigHolder.config().getLauncherFontFamily(), 12),
                        ConfigHolder.config().launcherFontFamilyProperty()));
                ConfigHolder.config().launcherFontFamilyProperty().addListener((a, b, newValue) -> {
                    Controllers.getScene().getStylesheets().setAll(Theme.getTheme().getStylesheets(newValue));
                });

                vbox.getChildren().add(lblFontDisplay);

                fontPane.getContent().add(vbox);
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("settings.launcher.font")), fontPane);
        }
    }
}
