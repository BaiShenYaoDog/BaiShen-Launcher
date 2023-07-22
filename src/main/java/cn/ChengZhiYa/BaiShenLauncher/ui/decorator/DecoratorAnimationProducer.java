package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationHandler;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationProducer;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.scene.Node;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DecoratorAnimationProducer implements AnimationProducer {
    @Override
    public void init(AnimationHandler handler) {
    }

    @Override
    public List<KeyFrame> animate(AnimationHandler handler) {
        Node prev = handler.getPreviousNode();
        Node next = handler.getCurrentNode();
        if (prev instanceof TransitionPane.EmptyPane) {
            return Collections.emptyList();
        }

        Duration halfDuration = handler.getDuration().divide(2);

        List<KeyFrame> keyFrames = new ArrayList<>();

        keyFrames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(prev.opacityProperty(), 1, Interpolator.EASE_BOTH)));
        keyFrames.add(new KeyFrame(halfDuration,
                new KeyValue(prev.opacityProperty(), 0, Interpolator.EASE_BOTH)));
        if (prev instanceof DecoratorAnimatedPage) {
            Node left = ((DecoratorAnimatedPage) prev).getLeft();
            Node center = ((DecoratorAnimatedPage) prev).getCenter();

            keyFrames.add(new KeyFrame(Duration.ZERO,
                    new KeyValue(left.translateXProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(center.translateXProperty(), 0, Interpolator.EASE_BOTH)));
            keyFrames.add(new KeyFrame(halfDuration,
                    new KeyValue(left.translateXProperty(), -30, Interpolator.EASE_BOTH),
                    new KeyValue(center.translateXProperty(), 30, Interpolator.EASE_BOTH)));
        }

        keyFrames.add(new KeyFrame(halfDuration,
                new KeyValue(next.opacityProperty(), 0, Interpolator.EASE_BOTH)));
        keyFrames.add(new KeyFrame(handler.getDuration(),
                new KeyValue(next.opacityProperty(), 1, Interpolator.EASE_BOTH)));
        if (next instanceof DecoratorAnimatedPage) {
            Node left = ((DecoratorAnimatedPage) next).getLeft();
            Node center = ((DecoratorAnimatedPage) next).getCenter();

            keyFrames.add(new KeyFrame(halfDuration,
                    new KeyValue(left.translateXProperty(), -30, Interpolator.EASE_BOTH),
                    new KeyValue(center.translateXProperty(), 30, Interpolator.EASE_BOTH)));
            keyFrames.add(new KeyFrame(handler.getDuration(),
                    new KeyValue(left.translateXProperty(), 0, Interpolator.EASE_BOTH),
                    new KeyValue(center.translateXProperty(), 0, Interpolator.EASE_BOTH)));
        }

        return keyFrames;
    }

    @Override
    public @Nullable AnimationProducer opposite() {
        return null;
    }
}
