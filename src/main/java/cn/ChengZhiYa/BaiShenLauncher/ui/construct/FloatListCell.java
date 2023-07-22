package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.effects.JFXDepthManager;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class FloatListCell<T> extends ListCell<T> {
    protected final StackPane pane = new StackPane();
    private final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    public FloatListCell(JFXListView<T> listView) {
        setText(null);
        setGraphic(null);

        pane.getStyleClass().add("card");
        pane.setCursor(Cursor.HAND);
        setPadding(new Insets(9, 9, 0, 9));
        JFXDepthManager.setDepth(pane, 1);

        FXUtils.onChangeAndOperate(selectedProperty(), selected -> {
            pane.pseudoClassStateChanged(SELECTED, selected);
        });

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
        updateControl(item, empty);
        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(pane);
        }
    }

    protected abstract void updateControl(T dataItem, boolean empty);
}
