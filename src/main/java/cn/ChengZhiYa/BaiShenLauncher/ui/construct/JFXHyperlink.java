package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import javafx.scene.control.Hyperlink;

public class JFXHyperlink extends Hyperlink {

    public JFXHyperlink() {
        super();

        setGraphic(SVG.launchOutline(Theme.blackFillBinding(), 16, 16));
    }

    public JFXHyperlink(String text) {
        super(text);

        setGraphic(SVG.launchOutline(Theme.blackFillBinding(), 16, 16));
    }

    public void setExternalLink(String externalLink) {
        this.setOnAction(e -> FXUtils.openLink(externalLink));
    }
}

