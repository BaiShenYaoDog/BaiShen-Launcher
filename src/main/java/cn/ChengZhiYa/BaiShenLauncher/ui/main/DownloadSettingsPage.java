package cn.ChengZhiYa.BaiShenLauncher.ui.main;

import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.DownloadProviders;
import cn.ChengZhiYa.BaiShenLauncher.task.FetchTask;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.ComponentList;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.HintPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Validator;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.ExtendedProperties;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.SafeStringConverter;
import com.jfoenix.controls.*;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;

import java.net.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.stringConverter;

public class DownloadSettingsPage extends StackPane {

    public DownloadSettingsPage() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setFillWidth(true);
        ScrollPane scrollPane = new ScrollPane(content);
        FXUtils.smoothScrolling(scrollPane);
        scrollPane.setFitToWidth(true);
        getChildren().setAll(scrollPane);

        {
            VBox downloadSource = new VBox(8);
            downloadSource.getStyleClass().add("card-non-transparent");
            {

                VBox chooseWrapper = new VBox();
                chooseWrapper.setPadding(new Insets(8, 0, 8, 0));
                JFXCheckBox chkAutoChooseDownloadSource = new JFXCheckBox(I18n.i18n("settings.launcher.download_source.auto"));
                chkAutoChooseDownloadSource.selectedProperty().bindBidirectional(ConfigHolder.config().autoChooseDownloadTypeProperty());
                chooseWrapper.getChildren().setAll(chkAutoChooseDownloadSource);

                BorderPane versionListSourcePane = new BorderPane();
                versionListSourcePane.setPadding(new Insets(0, 0, 8, 30));
                versionListSourcePane.disableProperty().bind(chkAutoChooseDownloadSource.selectedProperty().not());
                {
                    Label label = new Label(I18n.i18n("settings.launcher.version_list_source"));
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    versionListSourcePane.setLeft(label);

                    JFXComboBox<String> cboVersionListSource = new JFXComboBox<>();
                    cboVersionListSource.setConverter(stringConverter(key -> I18n.i18n("download.provider." + key)));
                    versionListSourcePane.setRight(cboVersionListSource);
                    FXUtils.setLimitWidth(cboVersionListSource, 400);

                    cboVersionListSource.getItems().setAll(DownloadProviders.providersById.keySet());
                    ExtendedProperties.selectedItemPropertyFor(cboVersionListSource).bindBidirectional(ConfigHolder.config().versionListSourceProperty());
                }

                BorderPane downloadSourcePane = new BorderPane();
                downloadSourcePane.setPadding(new Insets(0, 0, 8, 30));
                downloadSourcePane.disableProperty().bind(chkAutoChooseDownloadSource.selectedProperty());
                {
                    Label label = new Label(I18n.i18n("settings.launcher.download_source"));
                    BorderPane.setAlignment(label, Pos.CENTER_LEFT);
                    downloadSourcePane.setLeft(label);

                    JFXComboBox<String> cboDownloadSource = new JFXComboBox<>();
                    cboDownloadSource.setConverter(stringConverter(key -> I18n.i18n("download.provider." + key)));
                    downloadSourcePane.setRight(cboDownloadSource);
                    FXUtils.setLimitWidth(cboDownloadSource, 420);

                    cboDownloadSource.getItems().setAll(DownloadProviders.rawProviders.keySet());
                    ExtendedProperties.selectedItemPropertyFor(cboDownloadSource).bindBidirectional(ConfigHolder.config().downloadTypeProperty());
                }

                downloadSource.getChildren().setAll(chooseWrapper, versionListSourcePane, downloadSourcePane);
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("settings.launcher.version_list_source")), downloadSource);
        }

        {
            VBox downloadThreads = new VBox(16);
            downloadThreads.getStyleClass().add("card-non-transparent");
            {
                {
                    JFXCheckBox chkAutoDownloadThreads = new JFXCheckBox(I18n.i18n("settings.launcher.download.threads.auto"));
                    VBox.setMargin(chkAutoDownloadThreads, new Insets(8, 0, 0, 0));
                    chkAutoDownloadThreads.selectedProperty().bindBidirectional(ConfigHolder.config().autoDownloadThreadsProperty());
                    downloadThreads.getChildren().add(chkAutoDownloadThreads);

                    chkAutoDownloadThreads.selectedProperty().addListener((a, b, newValue) -> {
                        if (newValue) {
                            ConfigHolder.config().downloadThreadsProperty().set(FetchTask.DEFAULT_CONCURRENCY);
                        }
                    });
                }

                {
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.setPadding(new Insets(0, 0, 0, 30));
                    hbox.disableProperty().bind(ConfigHolder.config().autoDownloadThreadsProperty());
                    Label label = new Label(I18n.i18n("settings.launcher.download.threads"));

                    JFXSlider slider = new JFXSlider(1, 256, 64);
                    HBox.setHgrow(slider, Priority.ALWAYS);

                    JFXTextField threadsField = new JFXTextField();
                    FXUtils.setLimitWidth(threadsField, 60);
                    FXUtils.bindInt(threadsField, ConfigHolder.config().downloadThreadsProperty());

                    AtomicBoolean changedByTextField = new AtomicBoolean(false);
                    FXUtils.onChangeAndOperate(ConfigHolder.config().downloadThreadsProperty(), value -> {
                        changedByTextField.set(true);
                        slider.setValue(value.intValue());
                        changedByTextField.set(false);
                    });
                    slider.valueProperty().addListener((value, oldVal, newVal) -> {
                        if (changedByTextField.get()) return;
                        ConfigHolder.config().downloadThreadsProperty().set(value.getValue().intValue());
                    });

                    hbox.getChildren().setAll(label, slider, threadsField);
                    downloadThreads.getChildren().add(hbox);
                }

                {
                    HintPane hintPane = new HintPane(MessageDialogPane.MessageType.INFO);
                    VBox.setMargin(hintPane, new Insets(0, 0, 0, 30));
                    hintPane.disableProperty().bind(ConfigHolder.config().autoDownloadThreadsProperty());
                    hintPane.setText("并发数不是越高越好哦~");
                    downloadThreads.getChildren().add(hintPane);
                }
            }

            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("download")), downloadThreads);
        }

        {
            VBox proxyList = new VBox(10);
            proxyList.getStyleClass().add("card-non-transparent");

            VBox proxyPane = new VBox();
            {
                JFXCheckBox chkDisableProxy = new JFXCheckBox(I18n.i18n("settings.launcher.proxy.disable"));
                VBox.setMargin(chkDisableProxy, new Insets(8, 0, 0, 0));
                proxyList.getChildren().add(chkDisableProxy);
                ExtendedProperties.reversedSelectedPropertyFor(chkDisableProxy).bindBidirectional(ConfigHolder.config().hasProxyProperty());
                proxyPane.disableProperty().bind(chkDisableProxy.selectedProperty());
            }

            {
                proxyPane.setPadding(new Insets(0, 0, 0, 30));

                ColumnConstraints colHgrow = new ColumnConstraints();
                colHgrow.setHgrow(Priority.ALWAYS);

                JFXRadioButton chkProxyNone;
                JFXRadioButton chkProxyHttp;
                JFXRadioButton chkProxySocks;
                {
                    HBox hBox = new HBox();
                    chkProxyNone = new JFXRadioButton(I18n.i18n("settings.launcher.proxy.none"));
                    chkProxyHttp = new JFXRadioButton(I18n.i18n("settings.launcher.proxy.http"));
                    chkProxySocks = new JFXRadioButton(I18n.i18n("settings.launcher.proxy.socks"));
                    hBox.getChildren().setAll(chkProxyNone, chkProxyHttp, chkProxySocks);
                    proxyPane.getChildren().add(hBox);
                }

                {
                    GridPane gridPane = new GridPane();
                    gridPane.setHgap(20);
                    gridPane.setVgap(10);
                    gridPane.setStyle("-fx-padding: 0 0 0 15;");
                    gridPane.getColumnConstraints().setAll(new ColumnConstraints(), colHgrow);
                    gridPane.getRowConstraints().setAll(new RowConstraints(), new RowConstraints());

                    {
                        Label host = new Label(I18n.i18n("settings.launcher.proxy.host"));
                        GridPane.setRowIndex(host, 1);
                        GridPane.setColumnIndex(host, 0);
                        GridPane.setHalignment(host, HPos.RIGHT);
                        gridPane.getChildren().add(host);
                    }

                    {
                        JFXTextField txtProxyHost = new JFXTextField();
                        GridPane.setRowIndex(txtProxyHost, 1);
                        GridPane.setColumnIndex(txtProxyHost, 1);
                        gridPane.getChildren().add(txtProxyHost);
                        FXUtils.bindString(txtProxyHost, ConfigHolder.config().proxyHostProperty());
                    }

                    {
                        Label port = new Label(I18n.i18n("settings.launcher.proxy.port"));
                        GridPane.setRowIndex(port, 2);
                        GridPane.setColumnIndex(port, 0);
                        GridPane.setHalignment(port, HPos.RIGHT);
                        gridPane.getChildren().add(port);
                    }

                    {
                        JFXTextField txtProxyPort = new JFXTextField();
                        GridPane.setRowIndex(txtProxyPort, 2);
                        GridPane.setColumnIndex(txtProxyPort, 1);
                        FXUtils.setValidateWhileTextChanged(txtProxyPort, true);
                        gridPane.getChildren().add(txtProxyPort);

                        FXUtils.bind(txtProxyPort, ConfigHolder.config().proxyPortProperty(), SafeStringConverter.fromInteger()
                                .restrict(it -> it >= 0 && it <= 0xFFFF)
                                .fallbackTo(0)
                                .asPredicate(Validator.addTo(txtProxyPort)));
                    }
                    proxyPane.getChildren().add(gridPane);
                }

                GridPane authPane = new GridPane();
                {
                    VBox vBox = new VBox();
                    vBox.setStyle("-fx-padding: 20 0 20 5;");

                    JFXCheckBox chkProxyAuthentication = new JFXCheckBox(I18n.i18n("settings.launcher.proxy.authentication"));
                    vBox.getChildren().setAll(chkProxyAuthentication);
                    authPane.disableProperty().bind(chkProxyAuthentication.selectedProperty().not());
                    chkProxyAuthentication.selectedProperty().bindBidirectional(ConfigHolder.config().hasProxyAuthProperty());

                    proxyPane.getChildren().add(vBox);
                }

                {
                    authPane.setHgap(20);
                    authPane.setVgap(10);
                    authPane.setStyle("-fx-padding: 0 0 0 15;");
                    authPane.getColumnConstraints().setAll(new ColumnConstraints(), colHgrow);
                    authPane.getRowConstraints().setAll(new RowConstraints(), new RowConstraints());

                    {
                        Label username = new Label(I18n.i18n("settings.launcher.proxy.username"));
                        GridPane.setRowIndex(username, 0);
                        GridPane.setColumnIndex(username, 0);
                        authPane.getChildren().add(username);
                    }

                    {
                        JFXTextField txtProxyUsername = new JFXTextField();
                        GridPane.setRowIndex(txtProxyUsername, 0);
                        GridPane.setColumnIndex(txtProxyUsername, 1);
                        authPane.getChildren().add(txtProxyUsername);
                        FXUtils.bindString(txtProxyUsername, ConfigHolder.config().proxyUserProperty());
                    }

                    {
                        Label password = new Label(I18n.i18n("settings.launcher.proxy.password"));
                        GridPane.setRowIndex(password, 1);
                        GridPane.setColumnIndex(password, 0);
                        authPane.getChildren().add(password);
                    }

                    {
                        JFXPasswordField txtProxyPassword = new JFXPasswordField();
                        GridPane.setRowIndex(txtProxyPassword, 1);
                        GridPane.setColumnIndex(txtProxyPassword, 1);
                        authPane.getChildren().add(txtProxyPassword);
                        txtProxyPassword.textProperty().bindBidirectional(ConfigHolder.config().proxyPassProperty());
                    }

                    ToggleGroup proxyConfigurationGroup = new ToggleGroup();
                    chkProxyNone.setUserData(Proxy.Type.DIRECT);
                    chkProxyNone.setToggleGroup(proxyConfigurationGroup);
                    chkProxyHttp.setUserData(Proxy.Type.HTTP);
                    chkProxyHttp.setToggleGroup(proxyConfigurationGroup);
                    chkProxySocks.setUserData(Proxy.Type.SOCKS);
                    chkProxySocks.setToggleGroup(proxyConfigurationGroup);
                    ExtendedProperties.selectedItemPropertyFor(proxyConfigurationGroup, Proxy.Type.class).bindBidirectional(ConfigHolder.config().proxyTypeProperty());

                    proxyPane.getChildren().add(authPane);
                }
                proxyList.getChildren().add(proxyPane);
            }
            content.getChildren().addAll(ComponentList.createComponentListTitle(I18n.i18n("settings.launcher.proxy")), proxyList);
        }

    }
}
