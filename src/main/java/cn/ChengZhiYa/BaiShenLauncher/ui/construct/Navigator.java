package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.AnimationProducer;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.TransitionPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Navigation;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import java.util.Optional;
import java.util.Stack;
import java.util.logging.Level;

public class Navigator extends TransitionPane {
    private static final String PROPERTY_DIALOG_CLOSE_HANDLER = Navigator.class.getName() + ".closeListener";

    private final BooleanProperty backable = new SimpleBooleanProperty(this, "backable");
    private final Stack<Node> stack = new Stack<>();
    private boolean initialized = false;
    private ObjectProperty<EventHandler<NavigationEvent>> onNavigated = new SimpleObjectProperty<EventHandler<NavigationEvent>>(this, "onNavigated") {
        @Override
        protected void invalidated() {
            setEventHandler(NavigationEvent.NAVIGATED, get());
        }
    };
    private ObjectProperty<EventHandler<NavigationEvent>> onNavigating = new SimpleObjectProperty<EventHandler<NavigationEvent>>(this, "onNavigating") {
        @Override
        protected void invalidated() {
            setEventHandler(NavigationEvent.NAVIGATING, get());
        }
    };

    public void init(Node init) {
        stack.push(init);
        backable.set(canGoBack());
        getChildren().setAll(init);

        fireEvent(new NavigationEvent(this, init, Navigation.NavigationDirection.START, NavigationEvent.NAVIGATED));
        if (init instanceof PageAware) ((PageAware) init).onPageShown();

        initialized = true;
    }

    public void navigate(Node node, AnimationProducer animationProducer) {
        FXUtils.checkFxUserThread();

        if (!initialized)
            throw new IllegalStateException("Navigator must have a root page");

        Node from = stack.peek();
        if (from == node)
            return;

        Logging.LOG.info("Navigate to " + node);

        stack.push(node);
        backable.set(canGoBack());

        NavigationEvent navigating = new NavigationEvent(this, from, Navigation.NavigationDirection.NEXT, NavigationEvent.NAVIGATING);
        fireEvent(navigating);
        node.fireEvent(navigating);

        node.getProperties().put("hmcl.navigator.animation", animationProducer);
        setContent(node, animationProducer);

        NavigationEvent navigated = new NavigationEvent(this, node, Navigation.NavigationDirection.NEXT, NavigationEvent.NAVIGATED);
        node.fireEvent(navigated);
        if (node instanceof PageAware) ((PageAware) node).onPageShown();

        EventHandler<PageCloseEvent> handler = event -> close(node);
        node.getProperties().put(PROPERTY_DIALOG_CLOSE_HANDLER, handler);
        node.addEventHandler(PageCloseEvent.CLOSE, handler);
    }

    public void close() {
        close(stack.peek());
    }

    public void clear() {
        while (stack.size() > 1)
            close(stack.peek());
    }

    @SuppressWarnings("unchecked")
    public void close(Node from) {
        FXUtils.checkFxUserThread();

        if (!initialized)
            throw new IllegalStateException("Navigator must have a root page");

        if (stack.peek() != from) {
            // Allow page to be closed multiple times.
            Logging.LOG.log(Level.INFO, "Closing already closed page: " + from, new Throwable());
            return;
        }

        Logging.LOG.info("Closed page " + from);

        Node poppedNode = stack.pop();
        NavigationEvent exited = new NavigationEvent(this, poppedNode, Navigation.NavigationDirection.PREVIOUS, NavigationEvent.EXITED);
        poppedNode.fireEvent(exited);
        if (poppedNode instanceof PageAware) ((PageAware) poppedNode).onPageHidden();

        backable.set(canGoBack());
        Node node = stack.peek();

        NavigationEvent navigating = new NavigationEvent(this, from, Navigation.NavigationDirection.PREVIOUS, NavigationEvent.NAVIGATING);
        fireEvent(navigating);
        node.fireEvent(navigating);

        Object obj = from.getProperties().get("hmcl.navigator.animation");
        if (obj instanceof AnimationProducer) {
            setContent(node, (AnimationProducer) obj);
        } else {
            setContent(node, ContainerAnimations.NONE.getAnimationProducer());
        }

        NavigationEvent navigated = new NavigationEvent(this, node, Navigation.NavigationDirection.PREVIOUS, NavigationEvent.NAVIGATED);
        node.fireEvent(navigated);

        Optional.ofNullable(from.getProperties().get(PROPERTY_DIALOG_CLOSE_HANDLER))
                .ifPresent(handler -> from.removeEventHandler(PageCloseEvent.CLOSE, (EventHandler<PageCloseEvent>) handler));
    }

    public Node getCurrentPage() {
        return stack.peek();
    }

    public boolean canGoBack() {
        return stack.size() > 1;
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

    public int size() {
        return stack.size();
    }

    public void setContent(Node content, AnimationProducer animationProducer) {
        super.setContent(content, animationProducer);

        if (content instanceof Region) {
            ((Region) content).setMinSize(0, 0);
            FXUtils.setOverflowHidden((Region) content);
        }
    }

    public EventHandler<NavigationEvent> getOnNavigated() {
        return onNavigated.get();
    }

    public void setOnNavigated(EventHandler<NavigationEvent> onNavigated) {
        this.onNavigated.set(onNavigated);
    }

    public ObjectProperty<EventHandler<NavigationEvent>> onNavigatedProperty() {
        return onNavigated;
    }

    public EventHandler<NavigationEvent> getOnNavigating() {
        return onNavigating.get();
    }

    public void setOnNavigating(EventHandler<NavigationEvent> onNavigating) {
        this.onNavigating.set(onNavigating);
    }

    public ObjectProperty<EventHandler<NavigationEvent>> onNavigatingProperty() {
        return onNavigating;
    }

    public static class NavigationEvent extends Event {
        public static final EventType<NavigationEvent> EXITED = new EventType<>("EXITED");
        public static final EventType<NavigationEvent> NAVIGATED = new EventType<>("NAVIGATED");
        public static final EventType<NavigationEvent> NAVIGATING = new EventType<>("NAVIGATING");

        private final Navigator source;
        private final Node node;
        private final Navigation.NavigationDirection direction;

        public NavigationEvent(Navigator source, Node target, Navigation.NavigationDirection direction, EventType<? extends Event> eventType) {
            super(source, target, eventType);

            this.source = source;
            this.node = target;
            this.direction = direction;
        }

        @Override
        public Navigator getSource() {
            return source;
        }

        public Node getNode() {
            return node;
        }

        public Navigation.NavigationDirection getDirection() {
            return direction;
        }
    }
}
