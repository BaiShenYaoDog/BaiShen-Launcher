package cn.ChengZhiYa.BaiShenLauncher.ui.versions;

import cn.ChengZhiYa.BaiShenLauncher.mod.LocalModFile;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseForgeRemoteModRepository;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ModCheckUpdatesTask extends Task<List<LocalModFile.ModUpdate>> {

    private final String gameVersion;
    private final Collection<LocalModFile> mods;
    private final Collection<Task<LocalModFile.ModUpdate>> dependents;

    public ModCheckUpdatesTask(String gameVersion, Collection<LocalModFile> mods) {
        this.gameVersion = gameVersion;
        this.mods = mods;

        dependents = mods.stream()
                .map(mod -> Task.supplyAsync(() -> {
                    return mod.checkUpdates(gameVersion, CurseForgeRemoteModRepository.MODS);
                }).setSignificance(TaskSignificance.MAJOR).setName(mod.getFileName()).withCounter("mods.check_updates"))
                .collect(Collectors.toList());

        setStage("mods.check_updates");
        getProperties().put("total", dependents.size());
    }

    @Override
    public boolean doPreExecute() {
        return true;
    }

    @Override
    public void preExecute() {
        notifyPropertiesChanged();
    }

    @Override
    public Collection<? extends Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public boolean isRelyingOnDependents() {
        return false;
    }

    @Override
    public void execute() throws Exception {
        setResult(dependents.stream()
                .filter(task -> task.getResult() != null)
                .map(Task::getResult)
                .collect(Collectors.toList()));
    }
}
