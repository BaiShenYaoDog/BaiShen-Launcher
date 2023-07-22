package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.Holder;
import com.jfoenix.controls.JFXListView;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class MDListCell<T> extends ListCell<T> {
    private final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private final StackPane container = new StackPane();
    private final StackPane root = new StackPane();
    private final Holder<Object> lastCell;

    public MDListCell(JFXListView<T> listView, Holder<Object> lastCell) {
        this.lastCell = lastCell;

        setText(null);
        setGraphic(null);

        root.getStyleClass().add("md-list-cell");
        RipplerContainer ripplerContainer = new RipplerContainer(container);
        root.getChildren().setAll(ripplerContainer);

        Region clippedContainer = (Region) listView.lookup(".clipped-container");
        setPrefWidth(0);
        if (clippedContainer != null) {
            maxWidthProperty().bind(clippedContainer.widthProperty());
            prefWidthProperty().bind(clippedContainer.widthProperty());
            minWidthProperty().bind(clippedContainer.widthProperty());
        }
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        // https://mail.openjdk.org/pipermail/openjfx-dev/2022-July/034764.html
        if (lastCell != null) {
            if (this == lastCell.value && !isVisible())
                return;
            lastCell.value = this;
        }

        updateControl(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(root);
        }
    }

    protected StackPane getContainer() {
        return container;
    }

    protected void setSelectable() {
        FXUtils.onChangeAndOperate(selectedProperty(), selected -> {
            root.pseudoClassStateChanged(SELECTED, selected);
        });
    }

    protected abstract void updateControl(T item, boolean empty);
}
