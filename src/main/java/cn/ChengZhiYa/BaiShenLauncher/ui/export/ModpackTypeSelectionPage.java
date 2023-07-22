package cn.ChengZhiYa.BaiShenLauncher.ui.export;

import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackExportInfo;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.TwoLineListItem;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardPage;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.Map;

public final class ModpackTypeSelectionPage extends VBox implements WizardPage {
    public static final String MODPACK_TYPE = "modpack.type";
    public static final String MODPACK_TYPE_MCBBS = "mcbbs";
    public static final String MODPACK_TYPE_MULTIMC = "multimc";
    public static final String MODPACK_TYPE_SERVER = "server";
    private final WizardController controller;

    public ModpackTypeSelectionPage(WizardController controller) {
        this.controller = controller;

        Label title = new Label(I18n.i18n("modpack.export.as"));
        title.setPadding(new Insets(8));

        this.getStyleClass().add("jfx-list-view");
        this.setMaxSize(300, 150);
        this.getChildren().setAll(
                title,
                createButton(MODPACK_TYPE_MCBBS, McbbsModpackExportTask.OPTION),
                createButton(MODPACK_TYPE_MULTIMC, MultiMCModpackExportTask.OPTION),
                createButton(MODPACK_TYPE_SERVER, ServerModpackExportTask.OPTION)
        );
    }

    private JFXButton createButton(String type, ModpackExportInfo.Options option) {
        JFXButton button = new JFXButton();
        button.setOnAction(e -> {
            controller.getSettings().put(MODPACK_TYPE, type);
            controller.getSettings().put(ModpackInfoPage.MODPACK_INFO_OPTION, option);
            controller.onNext();
        });

        button.prefWidthProperty().bind(this.widthProperty());

        BorderPane graphic = new BorderPane();
        graphic.setMouseTransparent(true);
        graphic.setLeft(new TwoLineListItem(I18n.i18n("modpack.type." + type), I18n.i18n("modpack.type." + type + ".export")));

        SVGPath arrow = new SVGPath();
        arrow.setContent(SVG.ARROW_RIGHT);
        BorderPane.setAlignment(arrow, Pos.CENTER);
        graphic.setRight(arrow);

        button.setGraphic(graphic);

        return button;
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
    }

    @Override
    public String getTitle() {
        return I18n.i18n("modpack.wizard.step.3.title");
    }
}
