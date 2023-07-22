package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.upgrade.RemoteVersion;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.logging.Level;

import static cn.ChengZhiYa.BaiShenLauncher.Metadata.CHANGELOG_URL;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;

public class UpgradeDialog extends JFXDialogLayout {
    public UpgradeDialog(RemoteVersion remoteVersion, Runnable updateRunnable) {
        {
            setHeading(new Label(I18n.i18n("update.changelog")));
        }

        {
            String url = CHANGELOG_URL + remoteVersion.getChannel().channelName + ".html#nowchange";
            try {
                WebView webView = new WebView();
                webView.getEngine().setUserDataDirectory(Metadata.HMCL_DIRECTORY.toFile());
                WebEngine engine = webView.getEngine();
                engine.load(url);
                engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue == Worker.State.FAILED) {
                        Logging.LOG.warning("Failed to load update log, trying to open it in browser");
                        FXUtils.openLink(url);
                        setBody();
                    }
                });
                setBody(webView);
            } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
                Logging.LOG.log(Level.WARNING, "WebView is missing or initialization failed", e);
                FXUtils.openLink(url);
            }
        }

        {
            JFXButton updateButton = new JFXButton(I18n.i18n("update.accept"));
            updateButton.getStyleClass().add("dialog-accept");
            updateButton.setOnMouseClicked(e -> updateRunnable.run());

            JFXButton cancelButton = new JFXButton(I18n.i18n("button.cancel"));
            cancelButton.getStyleClass().add("dialog-cancel");
            cancelButton.setOnMouseClicked(e -> fireEvent(new DialogCloseEvent()));

            setActions(updateButton, cancelButton);
            onEscPressed(this, cancelButton::fire);
        }
    }
}
