package cn.ChengZhiYa.BaiShenLauncher.ui.animation;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public interface AnimationHandler {
    Duration getDuration();

    Pane getCurrentRoot();

    Node getPreviousNode();

    Node getCurrentNode();
}
