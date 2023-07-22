package cn.ChengZhiYa.BaiShenLauncher.ui.decorator;

import cn.ChengZhiYa.BaiShenLauncher.Launcher;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorDnD;
import cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder;
import cn.ChengZhiYa.BaiShenLauncher.setting.EnumBackgroundImage;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.AddAuthlibInjectorServerPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.animation.ContainerAnimations;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogAware;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.DialogCloseEvent;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.Navigator;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.StackContainerPane;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.Refreshable;
import cn.ChengZhiYa.BaiShenLauncher.ui.wizard.WizardProvider;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.io.FileUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSnackbar;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.newImage;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onEscPressed;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;

public class DecoratorController {
    private static final String PROPERTY_DIALOG_CLOSE_HANDLER = DecoratorController.class.getName() + ".dialog.closeListener";
    private static final DecoratorAnimationProducer animation = new DecoratorAnimationProducer();
    private final Decorator decorator;
    private final Navigator navigator;
    @SuppressWarnings("FieldCanBeLocal") // Strong reference
    private final InvalidationListener changeBackgroundListener;
    private JFXDialog dialog;
    private StackContainerPane dialogPane;

    // ==== Background ====
    //FXThread
    private int changeBackgroundCount = 0;
    private volatile Image defaultBackground;

    public DecoratorController(Stage stage, Node mainPage) {
        decorator = new Decorator(stage);
        decorator.setOnCloseButtonAction(Launcher::stopApplication);
        decorator.titleTransparentProperty().bind(ConfigHolder.config().titleTransparentProperty());

        navigator = new Navigator();
        navigator.setOnNavigated(this::onNavigated);
        navigator.init(mainPage);

        decorator.getContent().setAll(navigator);
        decorator.onCloseNavButtonActionProperty().set(e -> close());
        decorator.onBackNavButtonActionProperty().set(e -> back());
        decorator.onRefreshNavButtonActionProperty().set(e -> refresh());

        setupAuthlibInjectorDnD();

        // Setup background
        decorator.setContentBackground(getBackground());
        changeBackgroundListener = o -> {
            final int currentCount = ++this.changeBackgroundCount;
            CompletableFuture.supplyAsync(this::getBackground, Schedulers.io())
                    .thenAcceptAsync(background -> {
                        if (this.changeBackgroundCount == currentCount)
                            decorator.setContentBackground(background);
                    }, Schedulers.javafx());
        };
        WeakInvalidationListener weakListener = new WeakInvalidationListener(changeBackgroundListener);
        ConfigHolder.config().backgroundImageTypeProperty().addListener(weakListener);
        ConfigHolder.config().backgroundImageProperty().addListener(weakListener);
        ConfigHolder.config().backgroundImageUrlProperty().addListener(weakListener);

        // pass key events to current dialog / current page
        decorator.addEventFilter(KeyEvent.ANY, e -> {
            if (!(e.getTarget() instanceof Node)) {
                return; // event source can't be determined
            }

            Node newTarget;
            if (dialogPane != null && dialogPane.peek().isPresent()) {
                newTarget = dialogPane.peek().get(); // current dialog
            } else {
                newTarget = navigator.getCurrentPage(); // current page
            }

            boolean needsRedirect = true;
            Node t = (Node) e.getTarget();
            while (t != null) {
                if (t == newTarget) {
                    // current event target is in newTarget
                    needsRedirect = false;
                    break;
                }
                t = t.getParent();
            }
            if (!needsRedirect) {
                return;
            }

            e.consume();
            newTarget.fireEvent(e.copyFor(e.getSource(), newTarget));
        });

        // press ESC to go back
        onEscPressed(navigator, this::back);
    }

    public Decorator getDecorator() {
        return decorator;
    }

    private Background getBackground() {
        EnumBackgroundImage imageType = ConfigHolder.config().getBackgroundImageType();

        Image image = null;
        switch (imageType) {
            case JKOP:
                image = newImage("/assets/img/JKOP.png");
                break;
            case JKED:
                image = newImage("/assets/img/JKED.png");
                break;
            case YZOP:
                image = newImage("/assets/img/YZOP.png");
                break;
            case YZED:
                image = newImage("/assets/img/YZED.png");
                break;
            case XYZED:
                image = newImage("/assets/img/XYZED.png");
                break;
            case CUSTOM:
                String backgroundImage = ConfigHolder.config().getBackgroundImage();
                if (backgroundImage != null)
                    image = tryLoadImage(Paths.get(backgroundImage)).orElse(null);
                break;
            case NETWORK:
                String backgroundImageUrl = ConfigHolder.config().getBackgroundImageUrl();
                if (backgroundImageUrl != null && NetworkUtils.isURL(backgroundImageUrl))
                    image = tryLoadImage(backgroundImageUrl).orElse(null);
                break;
        }
        if (image == null) {
            image = loadDefaultBackgroundImage();
        }
        return new Background(new BackgroundImage(image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, new BackgroundSize(800, 480, false, false, true, true)));
    }

    /**
     * Load background image from bg/, background.png, background.jpg, background.gif
     */
    private Image loadDefaultBackgroundImage() {
        Optional<Image> image = randomImageIn(Paths.get("bg"));
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.png"));
        }
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.jpg"));
        }
        if (!image.isPresent()) {
            image = tryLoadImage(Paths.get("background.gif"));
        }

        return image.orElseGet(() -> {
            if (defaultBackground == null)
                defaultBackground = newImage("/assets/img/background.jpg");
            return defaultBackground;
        });
    }

    private Optional<Image> randomImageIn(Path imageDir) {
        if (!Files.isDirectory(imageDir)) {
            return Optional.empty();
        }

        List<Path> candidates;
        try (Stream<Path> stream = Files.list(imageDir)) {
            candidates = stream
                    .filter(Files::isReadable)
                    .filter(it -> {
                        String ext = FileUtils.getExtension(it).toLowerCase(Locale.ROOT);
                        return ext.equals("png") || ext.equals("jpg") || ext.equals("gif");
                    })
                    .collect(toList());
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Failed to list files in ./bg", e);
            return Optional.empty();
        }

        Random rnd = new Random();
        while (candidates.size() > 0) {
            int selected = rnd.nextInt(candidates.size());
            Optional<Image> loaded = tryLoadImage(candidates.get(selected));
            if (loaded.isPresent()) {
                return loaded;
            } else {
                candidates.remove(selected);
            }
        }
        return Optional.empty();
    }

    private Optional<Image> tryLoadImage(Path path) {
        if (!Files.isReadable(path))
            return Optional.empty();

        return tryLoadImage(path.toAbsolutePath().toUri().toString());
    }

    // ==== Navigation ====

    private Optional<Image> tryLoadImage(String url) {
        Image img;
        try {
            img = new Image(url);
        } catch (IllegalArgumentException e) {
            Logging.LOG.log(WARNING, "Couldn't load background image", e);
            return Optional.empty();
        }

        if (img.getException() != null) {
            Logging.LOG.log(WARNING, "Couldn't load background image", img.getException());
            return Optional.empty();
        }

        return Optional.of(img);
    }

    public void navigate(Node node) {
        navigator.navigate(node, animation);
    }

    private void close() {
        if (navigator.getCurrentPage() instanceof DecoratorPage) {
            DecoratorPage page = (DecoratorPage) navigator.getCurrentPage();

            if (page.isPageCloseable()) {
                page.closePage();
                return;
            }
        }
        navigator.clear();
    }

    private void back() {
        if (navigator.getCurrentPage() instanceof DecoratorPage) {
            DecoratorPage page = (DecoratorPage) navigator.getCurrentPage();

            if (page.back()) {
                if (navigator.canGoBack()) {
                    navigator.close();
                }
            }
        } else {
            if (navigator.canGoBack()) {
                navigator.close();
            }
        }
    }

    private void refresh() {
        if (navigator.getCurrentPage() instanceof Refreshable) {
            Refreshable refreshable = (Refreshable) navigator.getCurrentPage();

            if (refreshable.refreshableProperty().get())
                refreshable.refresh();
        }
    }

    private void onNavigated(Navigator.NavigationEvent event) {
        if (event.getSource() != this.navigator) return;
        Node to = event.getNode();

        if (to instanceof Refreshable) {
            decorator.canRefreshProperty().bind(((Refreshable) to).refreshableProperty());
        } else {
            decorator.canRefreshProperty().unbind();
            decorator.canRefreshProperty().set(false);
        }

        decorator.canCloseProperty().set(navigator.size() > 2);

        if (to instanceof DecoratorPage) {
            decorator.showCloseAsHomeProperty().set(!((DecoratorPage) to).isPageCloseable());
        } else {
            decorator.showCloseAsHomeProperty().set(true);
        }

        decorator.setNavigationDirection(event.getDirection());

        // state property should be updated at last.
        if (to instanceof DecoratorPage) {
            decorator.stateProperty().bind(((DecoratorPage) to).stateProperty());
        } else {
            decorator.stateProperty().unbind();
            decorator.stateProperty().set(new DecoratorPage.State("", null, navigator.canGoBack(), false, true));
        }

        if (to instanceof Region) {
            Region region = (Region) to;
            // Let root pane fix window size.
            StackPane parent = (StackPane) region.getParent();
            region.prefWidthProperty().bind(parent.widthProperty());
            region.prefHeightProperty().bind(parent.heightProperty());
        }
    }

    // ==== Dialog ====

    public void showDialog(Node node) {
        FXUtils.checkFxUserThread();

        if (dialog == null) {
            if (decorator.getDrawerWrapper() == null) {
                // Sometimes showDialog will be invoked before decorator was initialized.
                // Keep trying again.
                Platform.runLater(() -> showDialog(node));
                return;
            }
            dialog = new JFXDialog();
            dialogPane = new StackContainerPane();

            dialog.setContent(dialogPane);
            decorator.capableDraggingWindow(dialog);
            decorator.forbidDraggingWindow(dialogPane);
            dialog.setDialogContainer(decorator.getDrawerWrapper());
            dialog.setOverlayClose(false);
            dialog.show();

            navigator.setDisable(true);
        }
        dialogPane.push(node);

        EventHandler<DialogCloseEvent> handler = event -> closeDialog(node);
        node.getProperties().put(PROPERTY_DIALOG_CLOSE_HANDLER, handler);
        node.addEventHandler(DialogCloseEvent.CLOSE, handler);

        if (node instanceof DialogAware) {
            DialogAware dialogAware = (DialogAware) node;
            if (dialog.isVisible()) {
                dialogAware.onDialogShown();
            } else {
                dialog.visibleProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            dialogAware.onDialogShown();
                            observable.removeListener(this);
                        }
                    }
                });
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void closeDialog(Node node) {
        FXUtils.checkFxUserThread();

        Optional.ofNullable(node.getProperties().get(PROPERTY_DIALOG_CLOSE_HANDLER))
                .ifPresent(handler -> node.removeEventHandler(DialogCloseEvent.CLOSE, (EventHandler<DialogCloseEvent>) handler));

        if (dialog != null) {
            dialogPane.pop(node);

            if (node instanceof DialogAware) {
                ((DialogAware) node).onDialogClosed();
            }

            if (dialogPane.getChildren().isEmpty()) {
                dialog.close();
                dialog = null;
                dialogPane = null;

                navigator.setDisable(false);
            }
        }
    }

    // ==== Toast ====

    public void showToast(String content) {
        decorator.getSnackbar().fireEvent(new JFXSnackbar.SnackbarEvent(content, null, 2000L, false, null));
    }

    // ==== Wizard ====

    public void startWizard(WizardProvider wizardProvider) {
        startWizard(wizardProvider, null);
    }

    public void startWizard(WizardProvider wizardProvider, String category) {
        FXUtils.checkFxUserThread();

        navigator.navigate(new DecoratorWizardDisplayer(wizardProvider, category), ContainerAnimations.FADE.getAnimationProducer());
    }

    // ==== Authlib Injector DnD ====

    private void setupAuthlibInjectorDnD() {
        decorator.addEventFilter(DragEvent.DRAG_OVER, AuthlibInjectorDnD.dragOverHandler());
        decorator.addEventFilter(DragEvent.DRAG_DROPPED, AuthlibInjectorDnD.dragDroppedHandler(
                url -> Controllers.dialog(new AddAuthlibInjectorServerPane(url))));
    }
}
