package cn.ChengZhiYa.BaiShenLauncher.setting;

import cn.ChengZhiYa.BaiShenLauncher.auth.OAuth;
import cn.ChengZhiYa.BaiShenLauncher.task.Schedulers;
import cn.ChengZhiYa.BaiShenLauncher.task.Task;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.JsonUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.gson.UUIDTypeAdapter;
import cn.ChengZhiYa.BaiShenLauncher.util.io.HttpRequest;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import com.google.gson.annotations.SerializedName;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.UUID;

public final class HMCLAccounts {

    private static final ObjectProperty<HMCLAccount> account = new SimpleObjectProperty<>();

    private HMCLAccounts() {
    }

    public static HMCLAccount getAccount() {
        return account.get();
    }

    public static void setAccount(HMCLAccount account) {
        HMCLAccounts.account.set(account);
    }

    public static ObjectProperty<HMCLAccount> accountProperty() {
        return account;
    }

    public static Task<Void> login() {
        String nonce = UUIDTypeAdapter.fromUUID(UUID.randomUUID());
        String scope = "openid offline_access";

        return Task.supplyAsync(() -> {
            OAuth.Session session = Accounts.OAUTH_CALLBACK.startServer();
            Accounts.OAUTH_CALLBACK.openBrowser(NetworkUtils.withQuery(
                    "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
                    Lang.mapOf(
                            Pair.pair("client_id", Accounts.OAUTH_CALLBACK.getClientId()),
                            Pair.pair("response_type", "id_token code"),
                            Pair.pair("response_mode", "form_post"),
                            Pair.pair("scope", scope),
                            Pair.pair("redirect_uri", session.getRedirectURI()),
                            Pair.pair("nonce", nonce)
                    )));
            String code = session.waitFor();

            // Authorization Code -> Token
            String responseText = HttpRequest.POST("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                    .form(Lang.mapOf(Pair.pair("client_id", Accounts.OAUTH_CALLBACK.getClientId()), Pair.pair("code", code),
                            Pair.pair("grant_type", "authorization_code"), Pair.pair("client_secret", Accounts.OAUTH_CALLBACK.getClientSecret()),
                            Pair.pair("redirect_uri", session.getRedirectURI()), Pair.pair("scope", scope)))
                    .getString();
            OAuth.AuthorizationResponse response = JsonUtils.fromNonNullJson(responseText,
                    OAuth.AuthorizationResponse.class);

            HMCLAccountProfile profile = HttpRequest.GET("https://hmcl.huangyuhui.net/api/user")
                    .header("Token-Type", response.tokenType)
                    .header("Access-Token", response.accessToken)
                    .header("Authorization-Provider", "microsoft")
                    .authorization("Bearer", session.getIdToken())
                    .getJson(HMCLAccountProfile.class);

            return new HMCLAccount("microsoft", profile.nickname, profile.email, profile.role, session.getIdToken(), response.tokenType, response.accessToken, response.refreshToken);
        }).thenAcceptAsync(Schedulers.javafx(), account -> {
            setAccount(account);
        });
    }

    public static class HMCLAccount implements HttpRequest.Authorization {
        private final String provider;
        private final String nickname;
        private final String email;
        private final String role;
        private final String idToken;
        private final String tokenType;
        private final String accessToken;
        private final String refreshToken;

        public HMCLAccount(String provider, String nickname, String email, String role, String idToken, String tokenType, String accessToken, String refreshToken) {
            this.provider = provider;
            this.nickname = nickname;
            this.email = email;
            this.role = role;
            this.idToken = idToken;
            this.tokenType = tokenType;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getProvider() {
            return provider;
        }

        public String getNickname() {
            return nickname;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }

        public String getIdToken() {
            return idToken;
        }

        @Override
        public String getTokenType() {
            return tokenType;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    private static class HMCLAccountProfile {
        @SerializedName("ID")
        String id;
        @SerializedName("Provider")
        String provider;
        @SerializedName("Email")
        String email;
        @SerializedName("NickName")
        String nickname;
        @SerializedName("Role")
        String role;
    }

}
