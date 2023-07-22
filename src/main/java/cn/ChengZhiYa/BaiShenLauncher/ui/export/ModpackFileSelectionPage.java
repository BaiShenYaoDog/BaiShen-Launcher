package cn.ChengZhiYa.BaiShenLauncher.ui.export;

import cn.ChengZhiYa.BaiShenLauncher.mod.ModAdviser;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.NoneMultipleSelectionModel;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardController;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardPage;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTreeView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
 
public final class ModpackFileSelectionPage extends BorderPane implements WizardPage {
    public static final String MODPACK_FILE_SELECTION = "modpack.accepted";
    private static final Map<String, String> TRANSLATION = Lang.mapOf(
            Pair.pair("minecraft/hmclversion.cfg", I18n.i18n("modpack.files.hmclversion_cfg")),
            Pair.pair("minecraft/servers.dat", I18n.i18n("modpack.files.servers_dat")),
            Pair.pair("minecraft/saves", I18n.i18n("modpack.files.saves")),
            Pair.pair("minecraft/mods", I18n.i18n("modpack.files.mods")),
            Pair.pair("minecraft/config", I18n.i18n("modpack.files.config")),
            Pair.pair("minecraft/liteconfig", I18n.i18n("modpack.files.liteconfig")),
            Pair.pair("minecraft/resourcepacks", I18n.i18n("modpack.files.resourcepacks")),
            Pair.pair("minecraft/resources", I18n.i18n("modpack.files.resourcepacks")),
            Pair.pair("minecraft/options.txt", I18n.i18n("modpack.files.options_txt")),
            Pair.pair("minecraft/optionsshaders.txt", I18n.i18n("modpack.files.optionsshaders_txt")),
            Pair.pair("minecraft/mods/VoxelMods", I18n.i18n("modpack.files.mods.voxelmods")),
            Pair.pair("minecraft/dumps", I18n.i18n("modpack.files.dumps")),
            Pair.pair("minecraft/blueprints", I18n.i18n("modpack.files.blueprints")),
            Pair.pair("minecraft/scripts", I18n.i18n("modpack.files.scripts"))
    );
    private final WizardController controller;
    private final String version;
    private final ModAdviser adviser;
    private final CheckBoxTreeItem<String> rootNode;

    public ModpackFileSelectionPage(WizardController controller, Profile profile, String version, ModAdviser adviser) {
        this.controller = controller;
        this.version = version;
        this.adviser = adviser;

        JFXTreeView<String> treeView = new JFXTreeView<>();
        rootNode = getTreeItem(profile.getRepository().getRunDirectory(version), "minecraft");
        treeView.setRoot(rootNode);
        treeView.setSelectionModel(new NoneMultipleSelectionModel<>());
        this.setCenter(treeView);

        HBox nextPane = new HBox();
        nextPane.setPadding(new Insets(16, 16, 16, 0));
        nextPane.setAlignment(Pos.CENTER_RIGHT);
        {
            JFXButton btnNext = FXUtils.newRaisedButton(I18n.i18n("wizard.next"));
            btnNext.setPrefSize(100, 40);
            btnNext.setOnAction(e -> onNext());

            nextPane.getChildren().setAll(btnNext);
        }

        this.setBottom(nextPane);
    }

    private CheckBoxTreeItem<String> getTreeItem(File file, String basePath) {
        if (!file.exists())
            return null;

        ModAdviser.ModSuggestion state = ModAdviser.ModSuggestion.SUGGESTED;
        if (basePath.length() > "minecraft/".length()) {
            state = adviser.advise(StringUtils.substringAfter(basePath, "minecraft/") + (file.isDirectory() ? "/" : ""), file.isDirectory());
            if (file.isFile() && Objects.equals(FileUtils.getNameWithoutExtension(file), version)) // Ignore <version>.json, <version>.jar
                state = ModAdviser.ModSuggestion.HIDDEN;
            if (file.isDirectory() && Objects.equals(file.getName(), version + "-natives")) // Ignore <version>-natives
                state = ModAdviser.ModSuggestion.HIDDEN;
            if (state == ModAdviser.ModSuggestion.HIDDEN)
                return null;
        }

        CheckBoxTreeItem<String> node = new CheckBoxTreeItem<>(StringUtils.substringAfterLast(basePath, "/"));
        if (state == ModAdviser.ModSuggestion.SUGGESTED)
            node.setSelected(true);

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File it : files) {
                    CheckBoxTreeItem<String> subNode = getTreeItem(it, basePath + "/" + it.getName());
                    if (subNode != null) {
                        node.setSelected(subNode.isSelected() || node.isSelected());
                        if (!subNode.isSelected()) {
                            node.setIndeterminate(true);
                        }
                        node.getChildren().add(subNode);
                    }
                }
            }
            if (!node.isSelected()) node.setIndeterminate(false);

            // Empty folder need not to be displayed.
            if (node.getChildren().isEmpty()) {
                return null;
            }
        }

        HBox graphic = new HBox();
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().bindBidirectional(node.selectedProperty());
        checkBox.indeterminateProperty().bindBidirectional(node.indeterminateProperty());
        graphic.getChildren().add(checkBox);

        if (TRANSLATION.containsKey(basePath)) {
            Label comment = new Label();
            comment.setText(TRANSLATION.get(basePath));
            comment.setStyle("-fx-text-fill: gray;");
            comment.setMouseTransparent(true);
            graphic.getChildren().add(comment);
        }
        graphic.setPickOnBounds(false);
        node.setExpanded("minecraft".equals(basePath));
        node.setGraphic(graphic);

        return node;
    }

    private void getFilesNeeded(CheckBoxTreeItem<String> node, String basePath, List<String> list) {
        if (node == null) return;
        if (node.isSelected() || node.isIndeterminate()) {
            if (basePath.length() > "minecraft/".length())
                list.add(StringUtils.substringAfter(basePath, "minecraft/"));
            for (TreeItem<String> child : node.getChildren()) {
                if (child instanceof CheckBoxTreeItem) {
                    getFilesNeeded(((CheckBoxTreeItem<String>) child), basePath + "/" + child.getValue(), list);
                }
            }
        }
    }

    @Override
    public void cleanup(Map<String, Object> settings) {
        controller.getSettings().remove(MODPACK_FILE_SELECTION);
    }

    private void onNext() {
        ArrayList<String> list = new ArrayList<>();
        getFilesNeeded(rootNode, "minecraft", list);
        controller.getSettings().put(MODPACK_FILE_SELECTION, list);
        controller.onFinish();
    }

    @Override
    public String getTitle() {
        return I18n.i18n("modpack.wizard.step.2.title");
    }
}
