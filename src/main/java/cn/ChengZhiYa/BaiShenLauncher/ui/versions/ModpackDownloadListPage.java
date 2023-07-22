 package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.game.LocalizedRemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.mod.RemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseForgeRemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.mod.modrinth.ModrinthRemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;

public class ModpackDownloadListPage extends DownloadListPage {
    public ModpackDownloadListPage(DownloadPage.DownloadCallback callback, boolean versionSelection) {
        super(null, callback, versionSelection);

        repository = new Repository();

        supportChinese.set(true);
        downloadSources.get().setAll("mods.curseforge", "mods.modrinth");
        if (CurseForgeRemoteModRepository.isAvailable())
            downloadSource.set("mods.curseforge");
        else
            downloadSource.set("mods.modrinth");
    }

    @Override
    protected String getLocalizedCategory(String category) {
        if ("mods.modrinth".equals(downloadSource.get())) {
            return I18n.i18n("modrinth.category." + category);
        } else {
            return I18n.i18n("curse.category." + category);
        }
    }

    @Override
    protected String getLocalizedOfficialPage() {
        if ("mods.modrinth".equals(downloadSource.get())) {
            return I18n.i18n("mods.modrinth");
        } else {
            return I18n.i18n("mods.curseforge");
        }
    }

    private class Repository extends LocalizedRemoteModRepository {

        @Override
        protected RemoteModRepository getBackedRemoteModRepository() {
            if ("mods.modrinth".equals(downloadSource.get())) {
                return ModrinthRemoteModRepository.MODPACKS;
            } else {
                return CurseForgeRemoteModRepository.MODPACKS;
            }
        }

        @Override
        public RemoteModRepository.Type getType() {
            return RemoteModRepository.Type.MODPACK;
        }
    }
}
