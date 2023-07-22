package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.ui.construct.SpinnerPane;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Control;

public class ListPageBase<T> extends Control {
    private final ListProperty<T> items = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());
    private final BooleanProperty loading = new SimpleBooleanProperty(this, "loading", false);
    private final StringProperty failedReason = new SimpleStringProperty(this, "failed");
    private ObjectProperty<EventHandler<Event>> onFailedAction = new SimpleObjectProperty<EventHandler<Event>>(this, "onFailedAction") {
        @Override
        protected void invalidated() {
            setEventHandler(SpinnerPane.FAILED_ACTION, get());
        }
    };

    public ObservableList<T> getItems() {
        return items.get();
    }

    public void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    public ListProperty<T> itemsProperty() {
        return items;
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
}
