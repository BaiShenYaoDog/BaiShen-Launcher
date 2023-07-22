package cn.ChengZhiYa.BaiShenLauncher.game;

import cn.ChengZhiYa.BaiShenLauncher.auth.AuthenticationException;
import cn.ChengZhiYa.BaiShenLauncher.auth.OAuth;
import cn.ChengZhiYa.BaiShenLauncher.event.Event;
import cn.ChengZhiYa.BaiShenLauncher.event.EventManager;
import cn.ChengZhiYa.BaiShenLauncher.ui.FXUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.Lang;
import cn.ChengZhiYa.BaiShenLauncher.util.Logging;
import cn.ChengZhiYa.BaiShenLauncher.util.StringUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.i18n.I18n;
import cn.ChengZhiYa.BaiShenLauncher.util.io.IOUtils;
import cn.ChengZhiYa.BaiShenLauncher.util.io.NetworkUtils;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public final class OAuthServer extends NanoHTTPD implements OAuth.Session {
    public static String lastlyOpenedURL;
    private final int port;
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private String idToken;

    private OAuthServer(int port) {
        super(port);

        this.port = port;
    }

    @Override
    public String getRedirectURI() {
        return String.format("http://localhost:%d/auth-response", port);
    }

    @Override
    public String waitFor() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public String getIdToken() {
        return idToken;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!"/auth-response".equals(session.getUri())) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, "");
        }

        if (session.getMethod() == Method.POST) {
            Map<String, String> files = new HashMap<>();
            try {
                session.parseBody(files);
            } catch (IOException e) {
                Logging.LOG.log(Level.WARNING, "Failed to read post data", e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, "");
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        } else if (session.getMethod() == Method.GET) {
            // do nothing
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, "");
        }
        String parameters = session.getQueryParameterString();

        Map<String, String> query = Lang.mapOf(NetworkUtils.parseQuery(parameters));
        if (query.containsKey("code")) {
            idToken = query.get("id_token");
            future.complete(query.get("code"));
        } else {
            Logging.LOG.warning("Error: " + parameters);
            future.completeExceptionally(new AuthenticationException("failed to authenticate"));
        }

        String html;
        try {
            html = IOUtils.readFullyAsString(OAuthServer.class.getResourceAsStream("/assets/microsoft_auth.html"))
                    .replace("%close-page%", I18n.i18n("account.methods.microsoft.close_page"));
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "Failed to load html");
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, "");
        }
        Lang.thread(() -> {
            try {
                Thread.sleep(1000);
                stop();
            } catch (InterruptedException e) {
                Logging.LOG.log(Level.SEVERE, "Failed to sleep for 1 second");
            }
        });
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=UTF-8", html);
    }

    public static class Factory implements OAuth.Callback {
        public final EventManager<GrantDeviceCodeEvent> onGrantDeviceCode = new EventManager<>();
        public final EventManager<OpenBrowserEvent> onOpenBrowser = new EventManager<>();

        @Override
        public OAuth.Session startServer() throws IOException, AuthenticationException {
            if (StringUtils.isBlank(getClientId())) {
                throw new MicrosoftAuthenticationNotSupportedException();
            }

            IOException exception = null;
            for (int port : new int[]{29111, 29112, 29113, 29114, 29115}) {
                try {
                    OAuthServer server = new OAuthServer(port);
                    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
                    return server;
                } catch (IOException e) {
                    exception = e;
                }
            }
            throw exception;
        }

        @Override
        public void grantDeviceCode(String userCode, String verificationURI) {
            onGrantDeviceCode.fireEvent(new GrantDeviceCodeEvent(this, userCode, verificationURI));
        }

        @Override
        public void openBrowser(String url) throws IOException {
            lastlyOpenedURL = url;
            FXUtils.openLink(url);

            onOpenBrowser.fireEvent(new OpenBrowserEvent(this, url));
        }

        @Override
        public String getClientId() {
            //return "8b69da3a-1bce-46a4-bb5b-10ecdd59bab4";
            return "6a3728d6-27a3-4180-99bb-479895b8f88e";
        }

        @Override
        public String getClientSecret() {
            //return "c8d72b19-7cf7-4571-930f-ecc01ffc7048";
            return "dR.50SWwVez4-PQOF2-e_2GHmC~4Xl-p4p";
            //return "P6E8Q~o~_1sLzqzWfpO3lZ0OtuoaZx4oux6oNc1a";
        }

        @Override
        public boolean isPublicClient() {
            return true; // We have turned on the device auth flow.
        }
    }

    public static class GrantDeviceCodeEvent extends Event {
        private final String userCode;
        private final String verificationUri;

        public GrantDeviceCodeEvent(Object source, String userCode, String verificationUri) {
            super(source);
            this.userCode = userCode;
            this.verificationUri = verificationUri;
        }

        public String getUserCode() {
            return userCode;
        }

        public String getVerificationUri() {
            return verificationUri;
        }
    }

    public static class OpenBrowserEvent extends Event {
        private final String url;

        public OpenBrowserEvent(Object source, String url) {
            super(source);
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public static class MicrosoftAuthenticationNotSupportedException extends AuthenticationException {
    }
}
