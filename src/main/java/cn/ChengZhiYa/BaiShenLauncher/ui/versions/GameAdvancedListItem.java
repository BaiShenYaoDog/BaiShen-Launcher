package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.event.Event;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profile;
import cn.ChengZhiYa.BaiShenLauncher.setting.Profiles;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.WeakListenerHolder;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

import java.util.function.Consumer;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.newImage;

public class GameAdvancedListItem extends AdvancedListItem {
    private final Tooltip tooltip;
    private final ImageView imageView;
    private final WeakListenerHolder holder = new WeakListenerHolder();
    private Profile profile;
    private Consumer<Event> onVersionIconChangedListener;

    public GameAdvancedListItem() {
        tooltip = new Tooltip();

        Pair<Node, ImageView> view = createImageView(null);
        setLeftGraphic(view.getKey());
        imageView = view.getValue();

        holder.add(FXUtils.onWeakChangeAndOperate(Profiles.selectedVersionProperty(), this::loadVersion));

        setActionButtonVisible(false);
    }

    private void loadVersion(String version) {
        if (Profiles.getSelectedProfile() != profile) {
            profile = Profiles.getSelectedProfile();
            if (profile != null) {
                onVersionIconChangedListener = profile.getRepository().onVersionIconChanged.registerWeak(event -> {
                    this.loadVersion(Profiles.getSelectedVersion());
                });
            }
        }
        if (version != null && Profiles.getSelectedProfile() != null &&
                Profiles.getSelectedProfile().getRepository().hasVersion(version)) {
            FXUtils.installFastTooltip(this, tooltip);
            setTitle(version);
            setSubtitle(null);
            imageView.setImage(Profiles.getSelectedProfile().getRepository().getVersionIconImage(version));
            tooltip.setText(version);
        } else {
            Tooltip.uninstall(this, tooltip);
            setTitle("还没安装版本呢");
            setSubtitle(I18n.i18n("version.empty.add"));
            imageView.setImage(newImage("/assets/img/grass.png"));
            tooltip.setText("");
        }
    }
}
