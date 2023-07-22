package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Navigator;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TabControl;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TabHeader;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Navigation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.SingleSelectionModel;

public abstract class DecoratorTabPage extends DecoratorTransitionPage implements TabControl {

    private final ObjectProperty<SingleSelectionModel<TabHeader.Tab<?>>> selectionModel = new SimpleObjectProperty<>(this, "selectionModel", new TabControl.TabControlSelectionModel(this));
    private ObservableList<TabHeader.Tab<?>> tabs = FXCollections.observableArrayList();

    public DecoratorTabPage() {
        getSelectionModel().selectedItemProperty().addListener((a, b, newValue) -> {
            newValue.initializeIfNeeded();
            if (newValue.getNode() != null) {
                onNavigating(getCurrentPage());
                if (getCurrentPage() != null)
                    getCurrentPage().fireEvent(new Navigator.NavigationEvent(null, getCurrentPage(), Navigation.NavigationDirection.NEXT, Navigator.NavigationEvent.NAVIGATING));
                navigate(newValue.getNode(), ContainerAnimations.FADE.getAnimationProducer());
                onNavigated(getCurrentPage());
                if (getCurrentPage() != null)
                    getCurrentPage().fireEvent(new Navigator.NavigationEvent(null, getCurrentPage(), Navigation.NavigationDirection.NEXT, Navigator.NavigationEvent.NAVIGATED));
            }
        });
    }

    public DecoratorTabPage(TabHeader.Tab<?>... tabs) {
        this();
        if (tabs != null) {
            getTabs().addAll(tabs);
        }
    }

    @Override
    public ObservableList<TabHeader.Tab<?>> getTabs() {
        return tabs;
    }

    public SingleSelectionModel<TabHeader.Tab<?>> getSelectionModel() {
        return selectionModel.get();
    }

    public void setSelectionModel(SingleSelectionModel<TabHeader.Tab<?>> selectionModel) {
        this.selectionModel.set(selectionModel);
    }

    public ObjectProperty<SingleSelectionModel<TabHeader.Tab<?>>> selectionModelProperty() {
        return selectionModel;
    }
}
