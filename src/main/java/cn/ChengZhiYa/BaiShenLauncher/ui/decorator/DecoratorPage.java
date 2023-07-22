package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Navigator;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Refreshable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;

public interface DecoratorPage extends Refreshable {
    ReadOnlyObjectProperty<State> stateProperty();

    default boolean isPageCloseable() {
        return false;
    }

    default boolean back() {
        return true;
    }

    @Override
    default void refresh() {
    }

    default void closePage() {
    }

    default void onDecoratorPageNavigating(Navigator.NavigationEvent event) {
        ((Node) this).getStyleClass().add("content-background");
    }

    class State {
        private final String title;
        private final Node titleNode;
        private final boolean backable;
        private final boolean refreshable;
        private final boolean animate;
        private final double leftPaneWidth;

        public State(String title, Node titleNode, boolean backable, boolean refreshable, boolean animate) {
            this(title, titleNode, backable, refreshable, animate, 0);
        }

        public State(String title, Node titleNode, boolean backable, boolean refreshable, boolean animate, double leftPaneWidth) {
            this.title = title;
            this.titleNode = titleNode;
            this.backable = backable;
            this.refreshable = refreshable;
            this.animate = animate;
            this.leftPaneWidth = leftPaneWidth;
        }

        public static State fromTitle(String title) {
            return new State(title, null, true, false, true);
        }

        public static State fromTitle(String title, double leftPaneWidth) {
            return new State(title, null, true, false, true, leftPaneWidth);
        }

        public static State fromTitleNode(Node titleNode) {
            return new State(null, titleNode, true, false, true);
        }

        public String getTitle() {
            return title;
        }

        public Node getTitleNode() {
            return titleNode;
        }

        public boolean isBackable() {
            return backable;
        }

        public boolean isRefreshable() {
            return refreshable;
        }

        public boolean isAnimate() {
            return animate;
        }

        public double getLeftPaneWidth() {
            return leftPaneWidth;
        }
    }
}
