package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.BindingMapping;
import com.jfoenix.controls.JFXSpinner;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.StackPane;

@DefaultProperty("content")
public class SpinnerPane extends Control {
    public static final EventType<Event> FAILED_ACTION = new EventType<>(Event.ANY, "FAILED_ACTION");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content");
    private final BooleanProperty loading = new SimpleBooleanProperty(this, "loading");
    private final StringProperty failedReason = new SimpleStringProperty(this, "failedReason");
    private ObjectProperty<EventHandler<Event>> onFailedAction = new SimpleObjectProperty<EventHandler<Event>>(this, "onFailedAction") {
        @Override
        protected void invalidated() {
            setEventHandler(FAILED_ACTION, get());
        }
    };

    public SpinnerPane() {
        getStyleClass().add("spinner-pane");
    }

    public void showSpinner() {
        setLoading(true);
    }

    public void hideSpinner() {
        setFailedReason(null);
        setLoading(false);
    }

    public Node getContent() {
        return content.get();
    }

    public void setContent(Node content) {
        this.content.set(content);
    }

    public ObjectProperty<Node> contentProperty() {
        return content;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public String getFailedReason() {
        return failedReason.get();
    }

    public void setFailedReason(String failedReason) {
        this.failedReason.set(failedReason);
    }

    public StringProperty failedReasonProperty() {
        return failedReason;
    }

    public final ObjectProperty<EventHandler<Event>> onFailedActionProperty() {
        return onFailedAction;
    }

    public final EventHandler<Event> getOnFailedAction() {
        return onFailedActionProperty().get();
    }

    public final void setOnFailedAction(EventHandler<Event> value) {
        onFailedActionProperty().set(value);
    }

    @Override
    protected Skin createDefaultSkin() {
        return new Skin(this);
    }

    public interface State {
    }

    private static class Skin extends SkinBase<SpinnerPane> {
        private final JFXSpinner spinner = new JFXSpinner();
        private final StackPane contentPane = new StackPane();
        private final StackPane topPane = new StackPane();
        private final TransitionPane root = new TransitionPane();
        private final StackPane failedPane = new StackPane();
        private final Label failedReasonLabel = new Label();
        @SuppressWarnings("FieldCanBeLocal") // prevent from gc.
        private final InvalidationListener observer;

        protected Skin(SpinnerPane control) {
            super(control);

            topPane.getChildren().setAll(spinner);
            topPane.getStyleClass().add("notice-pane");
            failedPane.getStyleClass().add("notice-pane");
            failedPane.getChildren().setAll(failedReasonLabel);
            failedPane.onMouseClickedProperty().bind(
                    BindingMapping.of(control.onFailedAction)
                            .map(actionHandler -> (e -> {
                                if (actionHandler != null) {
                                    actionHandler.handle(new Event(FAILED_ACTION));
                                }
                            })));

            FXUtils.onChangeAndOperate(getSkinnable().content, newValue -> {
                if (newValue == null) {
                    contentPane.getChildren().clear();
                } else {
                    contentPane.getChildren().setAll(newValue);
                }
            });
            getChildren().setAll(root);

            observer = FXUtils.observeWeak(() -> {
                if (getSkinnable().getFailedReason() != null) {
                    root.setContent(failedPane, ContainerAnimations.FADE.getAnimationProducer());
                    failedReasonLabel.setText(getSkinnable().getFailedReason());
                } else if (getSkinnable().isLoading()) {
                    root.setContent(topPane, ContainerAnimations.FADE.getAnimationProducer());
                } else {
                    root.setContent(contentPane, ContainerAnimations.FADE.getAnimationProducer());
                }
            }, getSkinnable().loadingProperty(), getSkinnable().failedReasonProperty());
        }
    }

    public static class LoadedState implements State {
    }

    public static class LoadingState implements State {
    }

    public static class FailedState implements State {
        private final String reason;

        public FailedState(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
