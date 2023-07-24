package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import com.jfoenix.controls.JFXProgressBar;
import javafx.beans.binding.Bindings;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.newImage;

public class WebStage extends Stage {
    protected final StackPane pane = new StackPane();
    protected final JFXProgressBar progressBar = new JFXProgressBar();
    protected final WebView webView = new WebView();
    protected final WebEngine webEngine = webView.getEngine();

    public WebStage() {
        this(800, 480);
    }

    public WebStage(int width, int height) {
        setScene(new Scene(pane, width, height));
        getScene().getStylesheets().addAll(Theme.getTheme().getStylesheets(ConfigHolder.config().getLauncherFontFamily()));
        getIcons().add(newImage("/assets/img/icon.png"));
        webView.getEngine().setUserDataDirectory(Metadata.BSL_DIRECTORY.toFile());
        webView.setContextMenuEnabled(false);
        progressBar.progressProperty().bind(webView.getEngine().getLoadWorker().progressProperty());

        progressBar.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            switch (webView.getEngine().getLoadWorker().getState()) {
                case SUCCEEDED:
                case FAILED:
                case CANCELLED:
                    return false;
                default:
                    return true;
            }
        }, webEngine.getLoadWorker().stateProperty()));

        BorderPane borderPane = new BorderPane();
        borderPane.setPickOnBounds(false);
        borderPane.setTop(progressBar);
        progressBar.prefWidthProperty().bind(borderPane.widthProperty());
        pane.getChildren().setAll(webView, borderPane);
    }

    public WebView getWebView() {
        return webView;
    }
}
