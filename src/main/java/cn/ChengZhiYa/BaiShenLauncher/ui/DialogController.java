package cn.ChengZhiYa.BaiShenLauncher.ui;

import cn.ChengZhiYa.BaiShenLauncher.auth.*;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.ClassicAccountLoginDialog;
import cn.ChengZhiYa.BaiShenLauncher.ui.account.OAuthAccountLoginDialog;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils.runInFX;

public final class DialogController {
    private DialogController() {
    }

    public static AuthInfo logIn(Account account) throws CancellationException, AuthenticationException, InterruptedException {
        if (account instanceof ClassicAccount) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuthInfo> res = new AtomicReference<>(null);
            runInFX(() -> {
                ClassicAccountLoginDialog pane = new ClassicAccountLoginDialog((ClassicAccount) account, it -> {
                    res.set(it);
                    latch.countDown();
                }, latch::countDown);
                Controllers.dialog(pane);
            });
            latch.await();
            return Optional.ofNullable(res.get()).orElseThrow(CancellationException::new);
        } else if (account instanceof OAuthAccount) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<AuthInfo> res = new AtomicReference<>(null);
            runInFX(() -> {
                OAuthAccountLoginDialog pane = new OAuthAccountLoginDialog((OAuthAccount) account, it -> {
                    res.set(it);
                    latch.countDown();
                }, latch::countDown);
                Controllers.dialog(pane);
            });
            latch.await();
            return Optional.ofNullable(res.get()).orElseThrow(CancellationException::new);
        }
        return account.logIn();
    }
}
