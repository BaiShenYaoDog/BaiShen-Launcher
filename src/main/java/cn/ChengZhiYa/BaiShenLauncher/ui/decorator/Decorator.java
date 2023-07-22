package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Navigation;
import com.jfoenix.controls.JFXSnackbar;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Decorator extends Control {
    private final ListProperty<Node> drawer = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Node> content = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Node> container = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Background> contentBackground = new SimpleObjectProperty<>();
    private final ObjectProperty<DecoratorPage.State> state = new SimpleObjectProperty<>();
    private final StringProperty drawerTitle = new SimpleStringProperty();
    private final ObjectProperty<Runnable> onCloseButtonAction = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onCloseNavButtonAction = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onBackNavButtonAction = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRefreshNavButtonAction = new SimpleObjectProperty<>();
    private final BooleanProperty canRefresh = new SimpleBooleanProperty(false);
    private final BooleanProperty canBack = new SimpleBooleanProperty(false);
    private final BooleanProperty canClose = new SimpleBooleanProperty(false);
    private final BooleanProperty showCloseAsHome = new SimpleBooleanProperty(false);
    private final BooleanProperty titleTransparent = new SimpleBooleanProperty(true);
    private final Stage primaryStage;
    private final JFXSnackbar snackbar = new JFXSnackbar();
    private final ReadOnlyBooleanWrapper allowMove = new ReadOnlyBooleanWrapper();
    private final ReadOnlyBooleanWrapper dragging = new ReadOnlyBooleanWrapper();
    private Navigation.NavigationDirection navigationDirection = Navigation.NavigationDirection.START;
    private StackPane drawerWrapper;

    public Decorator(Stage primaryStage) {
        this.primaryStage = primaryStage;

        setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

        primaryStage.initStyle(StageStyle.UNDECORATED);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public StackPane getDrawerWrapper() {
        return drawerWrapper;
    }

    public void setDrawerWrapper(StackPane drawerWrapper) {
        this.drawerWrapper = drawerWrapper;
    }

    public ObservableList<Node> getDrawer() {
        return drawer.get();
    }

    public void setDrawer(ObservableList<Node> drawer) {
        this.drawer.set(drawer);
    }

    public ListProperty<Node> drawerProperty() {
        return drawer;
    }

    public ObservableList<Node> getContent() {
        return content.get();
    }

    public void setContent(ObservableList<Node> content) {
        this.content.set(content);
    }

    public ListProperty<Node> contentProperty() {
        return content;
    }

    public DecoratorPage.State getState() {
        return state.get();
    }

    public void setState(DecoratorPage.State state) {
        this.state.set(state);
    }

    public ObjectProperty<DecoratorPage.State> stateProperty() {
        return state;
    }

    public String getDrawerTitle() {
        return drawerTitle.get();
    }

    public void setDrawerTitle(String drawerTitle) {
        this.drawerTitle.set(drawerTitle);
    }

    public StringProperty drawerTitleProperty() {
        return drawerTitle;
    }

    public Runnable getOnCloseButtonAction() {
        return onCloseButtonAction.get();
    }

    public void setOnCloseButtonAction(Runnable onCloseButtonAction) {
        this.onCloseButtonAction.set(onCloseButtonAction);
    }

    public ObjectProperty<Runnable> onCloseButtonActionProperty() {
        return onCloseButtonAction;
    }

    public ObservableList<Node> getContainer() {
        return container.get();
    }

    public void setContainer(ObservableList<Node> container) {
        this.container.set(container);
    }

    public ListProperty<Node> containerProperty() {
        return container;
    }

    public Background getContentBackground() {
        return contentBackground.get();
    }

    public void setContentBackground(Background contentBackground) {
        this.contentBackground.set(contentBackground);
    }

    public ObjectProperty<Background> contentBackgroundProperty() {
        return contentBackground;
    }

    public BooleanProperty canRefreshProperty() {
        return canRefresh;
    }

    public BooleanProperty canBackProperty() {
        return canBack;
    }

    public BooleanProperty canCloseProperty() {
        return canClose;
    }

    public BooleanProperty showCloseAsHomeProperty() {
        return showCloseAsHome;
    }

    public boolean isAllowMove() {
        return allowMove.get();
    }

    void setAllowMove(boolean allowMove) {
        this.allowMove.set(allowMove);
    }

    public ReadOnlyBooleanProperty allowMoveProperty() {
        return allowMove.getReadOnlyProperty();
    }

    public boolean isDragging() {
        return dragging.get();
    }

    void setDragging(boolean dragging) {
        this.dragging.set(dragging);
    }

    public ReadOnlyBooleanProperty draggingProperty() {
        return dragging.getReadOnlyProperty();
    }

    public boolean isTitleTransparent() {
        return titleTransparent.get();
    }

    public void setTitleTransparent(boolean titleTransparent) {
        this.titleTransparent.set(titleTransparent);
    }

    public BooleanProperty titleTransparentProperty() {
        return titleTransparent;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onBackNavButtonActionProperty() {
        return onBackNavButtonAction;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCloseNavButtonActionProperty() {
        return onCloseNavButtonAction;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRefreshNavButtonActionProperty() {
        return onRefreshNavButtonAction;
    }

    public JFXSnackbar getSnackbar() {
        return snackbar;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DecoratorSkin(this);
    }

    public void minimize() {
        primaryStage.setIconified(true);
    }

    public void close() {
        onCloseButtonAction.get().run();
    }

    public void capableDraggingWindow(Node node) {
        node.addEventHandler(MouseEvent.MOUSE_MOVED, e -> allowMove.set(true));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (!isDragging()) allowMove.set(false);
        });
    }

    public void forbidDraggingWindow(Node node) {
        node.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            allowMove.set(false);
            e.consume();
        });
    }

    // TODO: Dirty implementation.
    public Navigation.NavigationDirection getNavigationDirection() {
        return navigationDirection;
    }

    public void setNavigationDirection(Navigation.NavigationDirection navigationDirection) {
        this.navigationDirection = navigationDirection;
    }
}
