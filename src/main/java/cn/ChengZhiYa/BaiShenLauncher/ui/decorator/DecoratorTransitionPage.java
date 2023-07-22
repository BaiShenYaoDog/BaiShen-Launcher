package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationProducer;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Refreshable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class DecoratorTransitionPage extends Control implements DecoratorPage {
    protected final ReadOnlyObjectWrapper<State> state = new ReadOnlyObjectWrapper<>(State.fromTitle(""));
    protected final TransitionPane transitionPane = new TransitionPane();
    private final DoubleProperty leftPaneWidth = new SimpleDoubleProperty();
    private final BooleanProperty backable = new SimpleBooleanProperty(false);
    private final BooleanProperty refreshable = new SimpleBooleanProperty(false);
    private Node currentPage;

    protected void navigate(Node page, AnimationProducer animation) {
        transitionPane.setContent(currentPage = page, animation);
    }

    protected void onNavigating(Node from) {
        if (from instanceof DecoratorPage)
            ((DecoratorPage) from).back();
    }

    protected void onNavigated(Node to) {
        if (to instanceof Refreshable) {
            refreshableProperty().bind(((Refreshable) to).refreshableProperty());
        } else {
            refreshableProperty().unbind();
            refreshableProperty().set(false);
        }

        if (to instanceof DecoratorPage) {
            state.bind(Bindings.createObjectBinding(() -> {
                State state = ((DecoratorPage) to).stateProperty().get();
                return new State(state.getTitle(), state.getTitleNode(), backable.get(), state.isRefreshable(), true, leftPaneWidth.get());
            }, ((DecoratorPage) to).stateProperty()));
        } else {
            state.unbind();
            state.set(new State("", null, backable.get(), false, true, leftPaneWidth.get()));
        }

        if (to instanceof Region) {
            Region region = (Region) to;
            // Let root pane fix window size.
            StackPane parent = (StackPane) region.getParent();
            region.prefWidthProperty().bind(parent.widthProperty());
            region.prefHeightProperty().bind(parent.heightProperty());
        }
    }

    @Override
    protected abstract Skin<?> createDefaultSkin();

    protected Node getCurrentPage() {
        return currentPage;
    }

    public boolean isBackable() {
        return backable.get();
    }

    public void setBackable(boolean backable) {
        this.backable.set(backable);
    }

    public BooleanProperty backableProperty() {
        return backable;
    }

    public boolean isRefreshable() {
        return refreshable.get();
    }

    public void setRefreshable(boolean refreshable) {
        this.refreshable.set(refreshable);
    }

    @Override
    public BooleanProperty refreshableProperty() {
        return refreshable;
    }

    @Override
    public ReadOnlyObjectProperty<State> stateProperty() {
        return state.getReadOnlyProperty();
    }

    public double getLeftPaneWidth() {
        return leftPaneWidth.get();
    }

    public void setLeftPaneWidth(double leftPaneWidth) {
        this.leftPaneWidth.set(leftPaneWidth);
    }

    public DoubleProperty leftPaneWidthProperty() {
        return leftPaneWidth;
    }
}
