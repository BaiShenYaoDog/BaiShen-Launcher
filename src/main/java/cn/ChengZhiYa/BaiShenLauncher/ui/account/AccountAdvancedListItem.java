package cn.ChengZhiYa.BaiShenLauncher.ui.account;

import cn.ChengZhiYa.BaiShenLauncher.auth.Account;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorAccount;
import cn.ChengZhiYa.BaiShenLauncher.auth.authlibinjector.AuthlibInjectorServer;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.TextureModel;
import cn.ChengZhiYa.BaiShenLauncher.auth.yggdrasil.YggdrasilAccount;
import cn.ChengZhiYa.BaiShenLauncher.game.TexturesLoader;
import cn.ChengZhiYa.BaiShenLauncher.setting.Accounts;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.ui.construct.AdvancedListItem;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.javafx.BindingMapping;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Tooltip;

import static javafx.beans.binding.Bindings.createStringBinding;

public class AccountAdvancedListItem extends AdvancedListItem {
    private final Tooltip tooltip;
    private final Canvas canvas;

    private final ObjectProperty<Account> account = new SimpleObjectProperty<Account>() {

        @Override
        protected void invalidated() {
            Account account = get();
            if (account == null) {
                titleProperty().unbind();
                subtitleProperty().unbind();
                tooltip.textProperty().unbind();
                setTitle("还没添加账号呢");
                setSubtitle(I18n.i18n("account.missing.add"));
                tooltip.setText(I18n.i18n("account.create"));

                TexturesLoader.unbindAvatar(canvas);
                TexturesLoader.drawAvatar(canvas, TexturesLoader.getDefaultSkin(TextureModel.STEVE).getImage());

            } else {
                titleProperty().bind(BindingMapping.of(account, Account::getCharacter));
                subtitleProperty().bind(accountSubtitle(account));
                tooltip.textProperty().bind(accountTooltip(account));
                TexturesLoader.bindAvatar(canvas, account);
            }
        }
    };

    public AccountAdvancedListItem() {
        tooltip = new Tooltip();
        FXUtils.installFastTooltip(this, tooltip);

        canvas = new Canvas(32, 32);
        setLeftGraphic(canvas);

        setActionButtonVisible(false);

        setOnScroll(event -> {
            Account current = account.get();
            if (current == null) return;
            ObservableList<Account> accounts = Accounts.getAccounts();
            int currentIndex = accounts.indexOf(account.get());
            if (event.getDeltaY() > 0) { // up
                currentIndex--;
            } else { // down
                currentIndex++;
            }
            Accounts.setSelectedAccount(accounts.get((currentIndex + accounts.size()) % accounts.size()));
        });
    }

    private static ObservableValue<String> accountSubtitle(Account account) {
        if (account instanceof AuthlibInjectorAccount) {
            return BindingMapping.of(((AuthlibInjectorAccount) account).getServer(), AuthlibInjectorServer::getName);
        } else {
            return createStringBinding(() -> Accounts.getLocalizedLoginTypeName(Accounts.getAccountFactory(account)));
        }
    }

    private static ObservableValue<String> accountTooltip(Account account) {
        if (account instanceof AuthlibInjectorAccount) {
            AuthlibInjectorServer server = ((AuthlibInjectorAccount) account).getServer();
            return Bindings.format("%s (%s) (%s)",
                    BindingMapping.of(account, Account::getCharacter),
                    account.getUsername(),
                    BindingMapping.of(server, AuthlibInjectorServer::getName));
        } else if (account instanceof YggdrasilAccount) {
            return Bindings.format("%s (%s)",
                    BindingMapping.of(account, Account::getCharacter),
                    account.getUsername());
        } else {
            return BindingMapping.of(account, Account::getCharacter);
        }
    }

    public ObjectProperty<Account> accountProperty() {
        return account;
    }

}
