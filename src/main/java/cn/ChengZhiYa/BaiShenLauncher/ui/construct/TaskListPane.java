package cn.ChengZhiYa.BaiShenLauncher.ui.construct;

import cn.ChengZhiYa.BaiShenLauncher.download.fabric.FabricAPIInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.fabric.FabricInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.forge.ForgeNewInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.forge.ForgeOldInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.game.GameAssetDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.download.game.GameInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.java.JavaDownloadTask;
import cn.ChengZhiYa.BaiShenLauncher.download.liteloader.LiteLoaderInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.optifine.OptiFineInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.quilt.QuiltAPIInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.download.quilt.QuiltInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.game.HMCLModpackInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.MinecraftInstanceTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.ModpackUpdateTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseCompletionTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.curse.CurseInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackCompletionTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.mcbbs.McbbsModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.modrinth.ModrinthCompletionTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.modrinth.ModrinthInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.multimc.MultiMCModpackInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackCompletionTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackExportTask;
import cn.ChengZhiYa.BaiShenLauncher.mod.server.ServerModpackLocalInstallTask;
import cn.ChengZhiYa.BaiShenLauncher.setting.Theme;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskExecutor;
import cn.ChengZhiYa.BaiShenLauncher.task.TaskListener;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.SVG;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public final class TaskListPane extends StackPane {
    private final AdvancedListBox listBox = new AdvancedListBox();
    private final Map<Task<?>, ProgressListNode> nodes = new HashMap<>();
    private final List<StageNode> stageNodes = new ArrayList<>();
    private final ObjectProperty<Insets> progressNodePadding = new SimpleObjectProperty<>(Insets.EMPTY);
    private TaskExecutor executor;

    public TaskListPane() {
        listBox.setSpacing(0);

        getChildren().setAll(listBox);
    }

    public void setExecutor(TaskExecutor executor) {
        List<String> stages = Lang.removingDuplicates(executor.getStages());
        this.executor = executor;
        executor.addTaskListener(new TaskListener() {
            @Override
            public void onStart() {
                Platform.runLater(() -> {
                    stageNodes.clear();
                    listBox.clear();
                    stageNodes.addAll(stages.stream().map(StageNode::new).collect(Collectors.toList()));
                    stageNodes.forEach(listBox::add);

                    if (stages.isEmpty()) progressNodePadding.setValue(new Insets(0, 0, 8, 0));
                    else progressNodePadding.setValue(new Insets(0, 0, 8, 26));
                });
            }

            @Override
            public void onReady(Task<?> task) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::begin);
                    });
                }
            }

            @Override
            public void onRunning(Task<?> task) {
                if (!task.getSignificance().shouldShow() || task.getName() == null)
                    return;

                if (task instanceof GameAssetDownloadTask) {
                    task.setName(I18n.i18n("assets.download_all"));
                } else if (task instanceof GameInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.game")));
                } else if (task instanceof ForgeNewInstallTask || task instanceof ForgeOldInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.forge")));
                } else if (task instanceof LiteLoaderInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.liteloader")));
                } else if (task instanceof OptiFineInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.optifine")));
                } else if (task instanceof FabricInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.fabric")));
                } else if (task instanceof FabricAPIInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.fabric-api")));
                } else if (task instanceof QuiltInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.quilt")));
                } else if (task instanceof QuiltAPIInstallTask) {
                    task.setName(I18n.i18n("install.installer.install", I18n.i18n("install.installer.quilt-api")));
                } else if (task instanceof CurseCompletionTask || task instanceof ModrinthCompletionTask || task instanceof ServerModpackCompletionTask || task instanceof McbbsModpackCompletionTask) {
                    task.setName(I18n.i18n("modpack.completion"));
                } else if (task instanceof ModpackInstallTask) {
                    task.setName(I18n.i18n("modpack.installing"));
                } else if (task instanceof ModpackUpdateTask) {
                    task.setName(I18n.i18n("modpack.update"));
                } else if (task instanceof CurseInstallTask) {
                    task.setName(I18n.i18n("modpack.install", I18n.i18n("modpack.type.curse")));
                } else if (task instanceof MultiMCModpackInstallTask) {
                    task.setName(I18n.i18n("modpack.install", I18n.i18n("modpack.type.multimc")));
                } else if (task instanceof ModrinthInstallTask) {
                    task.setName(I18n.i18n("modpack.install", I18n.i18n("modpack.type.modrinth")));
                } else if (task instanceof ServerModpackLocalInstallTask) {
                    task.setName(I18n.i18n("modpack.install", I18n.i18n("modpack.type.server")));
                } else if (task instanceof HMCLModpackInstallTask) {
                    task.setName(I18n.i18n("modpack.install", I18n.i18n("modpack.type.hmcl")));
                } else if (task instanceof McbbsModpackExportTask || task instanceof MultiMCModpackExportTask || task instanceof ServerModpackExportTask) {
                    task.setName(I18n.i18n("modpack.export"));
                } else if (task instanceof MinecraftInstanceTask) {
                    task.setName(I18n.i18n("modpack.scan"));
                } else if (task instanceof JavaDownloadTask) {
                    task.setName(I18n.i18n("download.java"));
                }

                Platform.runLater(() -> {
                    ProgressListNode node = new ProgressListNode(task);
                    nodes.put(task, node);
                    StageNode stageNode = stageNodes.stream().filter(x -> x.stage.equals(task.getInheritedStage())).findAny().orElse(null);
                    listBox.add(listBox.indexOf(stageNode) + 1, node);
                });
            }

            @Override
            public void onFinished(Task<?> task) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::succeed);
                    });
                }

                Platform.runLater(() -> {
                    ProgressListNode node = nodes.remove(task);
                    if (node == null)
                        return;
                    node.unbind();
                    listBox.remove(node);
                });
            }

            @Override
            public void onFailed(Task<?> task, Throwable throwable) {
                if (task.getStage() != null) {
                    Platform.runLater(() -> {
                        stageNodes.stream().filter(x -> x.stage.equals(task.getStage())).findAny().ifPresent(StageNode::fail);
                    });
                }
                ProgressListNode node = nodes.remove(task);
                if (node == null)
                    return;
                Platform.runLater(() -> {
                    node.setThrowable(throwable);
                });
            }

            @Override
            public void onPropertiesUpdate(Task<?> task) {
                if (task instanceof Task.CountTask) {
                    runInFX(() -> {
                        stageNodes.stream()
                                .filter(x -> x.stage.equals(((Task<?>.CountTask) task).getCountStage()))
                                .findAny()
                                .ifPresent(StageNode::count);
                    });

                    return;
                }

                if (task.getStage() != null) {
                    int total = Lang.tryCast(task.getProperties().get("total"), Integer.class).orElse(0);
                    runInFX(() -> {
                        stageNodes.stream()
                                .filter(x -> x.stage.equals(task.getStage()))
                                .findAny()
                                .ifPresent(stageNode -> {
                                    stageNode.setTotal(total);
                                });
                    });
                }
            }
        });
    }

    private static class StageNode extends BorderPane {
        private final String stage;
        private final Label title = new Label();
        private final String message;
        private int count = 0;
        private int total = 0;
        private boolean started = false;

        public StageNode(String stage) {
            this.stage = stage;

            String stageKey = StringUtils.substringBefore(stage, ':');
            String stageValue = StringUtils.substringAfter(stage, ':');

            // @formatter:off
            switch (stageKey) {
                case "hmcl.modpack": message = I18n.i18n("install.modpack"); break;
                case "hmcl.modpack.download": message = I18n.i18n("launch.state.modpack"); break;
                case "hmcl.install.assets": message = I18n.i18n("assets.download"); break;
                case "hmcl.install.game": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.game") + " " + stageValue); break;
                case "hmcl.install.forge": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.forge") + " " + stageValue); break;
                case "hmcl.install.liteloader": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.liteloader") + " " + stageValue); break;
                case "hmcl.install.optifine": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.optifine") + " " + stageValue); break;
                case "hmcl.install.fabric": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.fabric") + " " + stageValue); break;
                case "hmcl.install.fabric-api": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.fabric-api") + " " + stageValue); break;
                case "hmcl.install.quilt": message = I18n.i18n("install.installer.install", I18n.i18n("install.installer.quilt") + " " + stageValue); break;
                default: message = I18n.i18n(stageKey); break;
            }
            // @formatter:on

            title.setText(message);
            BorderPane.setAlignment(title, Pos.CENTER_LEFT);
            BorderPane.setMargin(title, new Insets(0, 0, 0, 8));
            setPadding(new Insets(0, 0, 8, 4));
            setCenter(title);
            setLeft(FXUtils.limitingSize(SVG.dotsHorizontal(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void begin() {
            if (started) return;
            started = true;
            setLeft(FXUtils.limitingSize(SVG.arrowRight(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void fail() {
            setLeft(FXUtils.limitingSize(SVG.close(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void succeed() {
            setLeft(FXUtils.limitingSize(SVG.check(Theme.blackFillBinding(), 14, 14), 14, 14));
        }

        public void count() {
            updateCounter(++count, total);
        }

        public void setTotal(int total) {
            this.total = total;
            updateCounter(count, total);
        }

        public void updateCounter(int count, int total) {
            if (total > 0)
                title.setText(String.format("%s - %d/%d", message, count, total));
            else
                title.setText(message);
        }
    }

    private class ProgressListNode extends BorderPane {
        private final JFXProgressBar bar = new JFXProgressBar();
        private final Label title = new Label();
        private final Label state = new Label();
        private final DoubleBinding binding = Bindings.createDoubleBinding(() ->
                        getWidth() - getPadding().getLeft() - getPadding().getRight(),
                paddingProperty(), widthProperty());

        public ProgressListNode(Task<?> task) {
            bar.progressProperty().bind(task.progressProperty());
            title.setText(task.getName());
            state.textProperty().bind(task.messageProperty());

            setLeft(title);
            setRight(state);
            setBottom(bar);

            bar.minWidthProperty().bind(binding);
            bar.prefWidthProperty().bind(binding);
            bar.maxWidthProperty().bind(binding);

            paddingProperty().bind(progressNodePadding);
        }

        public void unbind() {
            bar.progressProperty().unbind();
            state.textProperty().unbind();
        }

        public void setThrowable(Throwable throwable) {
            unbind();
            state.setText(throwable.getLocalizedMessage());
            bar.setProgress(0);
        }
    }
}
