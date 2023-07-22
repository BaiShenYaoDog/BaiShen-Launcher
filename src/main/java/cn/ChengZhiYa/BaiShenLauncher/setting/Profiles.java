package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.Metadata;
import cn.ChengZhiYa.BaiShenLauncher.event.EventBus;
import cn.ChengZhiYa.BaiShenLauncher.event.RefreshedVersionsEvent;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

import static cn.ChengZhiYa.BaiShenLauncher.setting.ConfigHolder.config;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.onInvalidating;
import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;
import static cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n.i18n;
import static javafx.collections.FXCollections.observableArrayList;

public final class Profiles {

    public static final String DEFAULT_PROFILE = "Default";
    public static final String HOME_PROFILE = "Home";
    private static final ObservableList<Profile> profiles = observableArrayList(profile -> new Observable[]{profile});
    private static final ReadOnlyListWrapper<Profile> profilesWrapper = new ReadOnlyListWrapper<>(profiles);
    private static final ReadOnlyStringWrapper selectedVersion = new ReadOnlyStringWrapper();
    private static final List<Consumer<Profile>> versionsListeners = new ArrayList<>(4);
    /**
     * True if {@link #init()} hasn't been called.
     */
    private static boolean initialized = false;
    private static ObjectProperty<Profile> selectedProfile = new SimpleObjectProperty<Profile>() {
        {
            profiles.addListener(onInvalidating(this::invalidated));
        }

        @Override
        protected void invalidated() {
            if (!initialized)
                return;

            Profile profile = get();

            if (profiles.isEmpty()) {
                if (profile != null) {
                    set(null);
                    return;
                }
            } else {
                if (!profiles.contains(profile)) {
                    set(profiles.get(0));
                    return;
                }
            }

            config().setSelectedProfile(profile == null ? "" : profile.getName());
            if (profile != null) {
                if (profile.getRepository().isLoaded())
                    selectedVersion.bind(profile.selectedVersionProperty());
                else {
                    selectedVersion.unbind();
                    selectedVersion.set(null);
                    // bind when repository was reloaded.
                    profile.getRepository().refreshVersionsAsync().start();
                }
            } else {
                selectedVersion.unbind();
                selectedVersion.set(null);
            }
        }
    };

    static {
        profiles.addListener(onInvalidating(Profiles::updateProfileStorages));
        profiles.addListener(onInvalidating(Profiles::checkProfiles));

        selectedProfile.addListener((a, b, newValue) -> {
            if (newValue != null)
                newValue.getRepository().refreshVersionsAsync().start();
        });
    }

    private Profiles() {
    }

    public static String getProfileDisplayName(Profile profile) {
        switch (profile.getName()) {
            case Profiles.DEFAULT_PROFILE:
                return i18n("profile.default");
            case Profiles.HOME_PROFILE:
                return i18n("profile.home");
            default:
                return profile.getName();
        }
    }

    private static void checkProfiles() {
        if (profiles.isEmpty()) {
            Profile current = new Profile(Profiles.DEFAULT_PROFILE, new File(".minecraft"), new VersionSetting(), null, true);
            Profile home = new Profile(Profiles.HOME_PROFILE, Metadata.MINECRAFT_DIRECTORY.toFile());
            Platform.runLater(() -> profiles.addAll(current, home));
        }
    }

    private static void updateProfileStorages() {
        // don't update the underlying storage before data loading is completed
        // otherwise it might cause data loss
        if (!initialized)
            return;
        // update storage
        TreeMap<String, Profile> newConfigurations = new TreeMap<>();
        for (Profile profile : profiles) {
            newConfigurations.put(profile.getName(), profile);
        }
        config().getConfigurations().setValue(FXCollections.observableMap(newConfigurations));
    }

    /**
     * Called when it's ready to load profiles from {@link ConfigHolder#config()}.
     */
    static void init() {
        if (initialized)
            throw new IllegalStateException("Already initialized");

        HashSet<String> names = new HashSet<>();
        config().getConfigurations().forEach((name, profile) -> {
            if (!names.add(name)) return;
            profiles.add(profile);
            profile.setName(name);
        });
        checkProfiles();

        // Platform.runLater is necessary or profiles will be empty
        // since checkProfiles adds 2 base profile later.
        Platform.runLater(() -> {
            initialized = true;

            selectedProfile.set(
                    profiles.stream()
                            .filter(it -> it.getName().equals(config().getSelectedProfile()))
                            .findFirst()
                            .orElse(profiles.get(0)));
        });

        EventBus.EVENT_BUS.channel(RefreshedVersionsEvent.class).registerWeak(event -> {
            runInFX(() -> {
                Profile profile = selectedProfile.get();
                if (profile != null && profile.getRepository() == event.getSource()) {
                    selectedVersion.bind(profile.selectedVersionProperty());
                    for (Consumer<Profile> listener : versionsListeners)
                        listener.accept(profile);
                }
            });
        });
    }

    public static ObservableList<Profile> getProfiles() {
        return profiles;
    }

    public static ReadOnlyListProperty<Profile> profilesProperty() {
        return profilesWrapper.getReadOnlyProperty();
    }

    public static Profile getSelectedProfile() {
        return selectedProfile.get();
    }

    public static void setSelectedProfile(Profile profile) {
        selectedProfile.set(profile);
    }

    public static ObjectProperty<Profile> selectedProfileProperty() {
        return selectedProfile;
    }

    public static ReadOnlyStringProperty selectedVersionProperty() {
        return selectedVersion.getReadOnlyProperty();
    }

    // Guaranteed that the repository is loaded.
    public static String getSelectedVersion() {
        return selectedVersion.get();
    }

    public static void registerVersionsListener(Consumer<Profile> listener) {
        Profile profile = getSelectedProfile();
        if (profile != null && profile.getRepository().isLoaded())
            listener.accept(profile);
        versionsListeners.add(listener);
    }
}
