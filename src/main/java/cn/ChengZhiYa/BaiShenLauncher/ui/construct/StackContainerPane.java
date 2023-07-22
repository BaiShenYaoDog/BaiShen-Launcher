package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.Optional;
import java.util.Stack;

public class StackContainerPane extends StackPane {
    private final Stack<Node> stack = new Stack<>();

    public Optional<Node> peek() {
        if (stack.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(stack.peek());
        }
    }

    public void push(Node node) {
        stack.push(node);
        getChildren().setAll(node);

        Logging.LOG.info(this + " " + stack);
    }

    public void pop(Node node) {
        boolean flag = stack.remove(node);
        if (stack.isEmpty())
            getChildren().setAll();
        else
            getChildren().setAll(stack.peek());

        Logging.LOG.info(this + " " + stack + ", removed: " + flag + ", object: " + node);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}
