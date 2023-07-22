package cn.ChengZhiYa.BaiShenLauncher.ui.account;

import cn.ChengZhiYa.BaiShenLauncher.auth.Account;
import cn.ChengZhiYa.BaiShenLauncher.auth.AuthenticationException;
import cn.ChengZhiYa.BaiShenLauncher.auth.CredentialExpiredException;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.auth.microsoft.MicrosoftAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.offline.OfflineAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.CompleteGameProfile;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.TextureType;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.YggdrasilAccount;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.ui.Controllers;
import cn.ChengZhiYa.BaiShenLauncher.ui.DialogController;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.MessageDialogPane;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.skin.InvalidSkinException;
import cn.ChengZhiYa.BaiShenLauncher.util.skin.NormalizedSkin;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import static java.util.Collections.emptySet;
import static javafx.beans.binding.Bindings.createBooleanBinding;

public class AccountListItem extends RadioButton {

    private final Account account;
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty subtitle = new SimpleStringProperty();

    public AccountListItem(Account account) {
        this.account = account;
        getStyleClass().clear();
        setUserData(account);

        String loginTypeName = Accounts.getLocalizedLoginTypeName(Accounts.getAccountFactory(account));
        String portableSuffix = account.isPortable() ? ", " + I18n.i18n("account.portable") : "";
        if (account instanceof AuthlibInjectorAccount) {
            AuthlibInjectorServer server = ((AuthlibInjectorAccount) account).getServer();
            subtitle.bind(Bindings.concat(
                    loginTypeName, ", ", I18n.i18n("account.injector.server"), ": ",
                    Bindings.createStringBinding(server::getName, server), portableSuffix));
        } else {
            subtitle.set(loginTypeName + portableSuffix);
        }

        StringBinding characterName = Bindings.createStringBinding(account::getCharacter, account);
        if (account instanceof OfflineAccount) {
            title.bind(characterName);
        } else {
            title.bind(
                    account.getUsername().isEmpty() ? characterName :
                            Bindings.concat(account.getUsername(), " - ", characterName));
        }
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new AccountListItemSkin(this);
    }

    public Task<?> refreshAsync() {
        return Task.runAsync(() -> {
            account.clearCache();
            try {
                account.logIn();
            } catch (CredentialExpiredException e) {
                try {
                    DialogController.logIn(account);
                } catch (CancellationException e1) {
                    // ignore cancellation
                } catch (Exception e1) {
                    Logging.LOG.log(Level.WARNING, "Failed to refresh " + account + " with password", e1);
                    throw e1;
                }
            } catch (AuthenticationException e) {
                Logging.LOG.log(Level.WARNING, "Failed to refresh " + account + " with token", e);
                throw e;
            }
        });
    }

    public ObservableBooleanValue canUploadSkin() {
        if (account instanceof YggdrasilAccount) {
            if (account instanceof AuthlibInjectorAccount) {
                AuthlibInjectorAccount aiAccount = (AuthlibInjectorAccount) account;
                ObjectBinding<Optional<CompleteGameProfile>> profile = aiAccount.getYggdrasilService().getProfileRepository().binding(aiAccount.getUUID());
                return createBooleanBinding(() -> {
                    Set<TextureType> uploadableTextures = profile.get()
                            .map(AuthlibInjectorAccount::getUploadableTextures)
                            .orElse(emptySet());
                    return uploadableTextures.contains(TextureType.SKIN);
                }, profile);
            } else {
                return createBooleanBinding(() -> true);
            }
        } else if (account instanceof OfflineAccount || account instanceof MicrosoftAccount) {
            return createBooleanBinding(() -> true);
        } else {
            return createBooleanBinding(() -> false);
        }
    }

    /**
     * @return the skin upload task, null if no file is selected
     */
    @Nullable
    public Task<?> uploadSkin() {
        if (account instanceof OfflineAccount) {
            Controllers.dialog(new OfflineAccountSkinPane((OfflineAccount) account));
            return null;
        }
        if (account instanceof MicrosoftAccount) {
            FXUtils.openLink("https://www.minecraft.net/msaprofile/mygames/editskin");
            return null;
        }
        if (!(account instanceof YggdrasilAccount)) {
            return null;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.i18n("account.skin.upload"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.i18n("account.skin.file"), "*.png"));
        File selectedFile = chooser.showOpenDialog(Controllers.getStage());
        if (selectedFile == null) {
            return null;
        }

        return refreshAsync()
                .thenRunAsync(() -> {
                    Image skinImg;
                    try (FileInputStream input = new FileInputStream(selectedFile)) {
                        skinImg = new Image(input);
                    } catch (IOException e) {
                        throw new InvalidSkinException("Failed to read skin image", e);
                    }
                    if (skinImg.isError()) {
                        throw new InvalidSkinException("Failed to read skin image", skinImg.getException());
                    }
                    NormalizedSkin skin = new NormalizedSkin(skinImg);
                    String model = skin.isSlim() ? "slim" : "";
                    Logging.LOG.info("Uploading skin [" + selectedFile + "], model [" + model + "]");
                    ((YggdrasilAccount) account).uploadSkin(model, selectedFile.toPath());
                })
                .thenComposeAsync(refreshAsync())
                .whenComplete(Schedulers.javafx(), e -> {
                    if (e != null) {
                        Controllers.dialog(Accounts.localizeErrorMessage(e), I18n.i18n("account.skin.upload.failed"), MessageDialogPane.MessageType.ERROR);
                    }
                });
    }

    public void remove() {
        Accounts.getAccounts().remove(account);
    }

    public Account getAccount() {
        return account;
    }

    public String getTitle() {
        return title.get();
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getSubtitle() {
        return subtitle.get();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle.set(subtitle);
    }

    public StringProperty subtitleProperty() {
        return subtitle;
    }
}
