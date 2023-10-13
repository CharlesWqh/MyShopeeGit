package com.shopee.shopeegit.gitlab.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = Settings.NAME, storages = @Storage("gitlab-quickmr.xml"))
public class Settings implements PersistentStateComponent<Settings.State> {
    public static final String NAME = "gitlab-quickmr";

    private State state = new State();

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isInitialized() {
        return this.state.gitLabUri != null
                && this.getAccessToken() != null
                && this.state.webhookUrl != null;
    }

    public void reset() {
        this.state = new State();
    }

    public String getGitLabUri() {
        return this.state.gitLabUri;
    }

    public void setGitLabUri(String gitLabUri) {
        this.state.gitLabUri = gitLabUri;
    }

    public String getAccessToken() {
        CredentialAttributes credentialAttributes = getCredentialAttributes();
        if (credentialAttributes == null) {
            return null;
        }
        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials == null) {
            return null;
        }
        return credentials.getPasswordAsString();
    }

    public void setAccessToken(String accessToken) {
        CredentialAttributes credentialAttributes = getCredentialAttributes();
        if (credentialAttributes == null) {
            return;
        }
        PasswordSafe.getInstance().setPassword(credentialAttributes, accessToken);
    }

    public String getWebhookUrl() {
        return this.state.webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.state.webhookUrl = webhookUrl;
    }

    public boolean isInsecureTls() {
        return this.state.insecureTls;
    }

    public void setInsecureTls(boolean insecureTls) {
        this.state.insecureTls = insecureTls;
    }


    private CredentialAttributes getCredentialAttributes() {
        if (getGitLabUri() == null) {
            return null;
        }
        String serviceName = NAME + "\t" + getGitLabUri();
        String userName = getGitLabUri();
        return new CredentialAttributes(serviceName, userName, this.getClass(), false);
    }

    public static class State {
        public String gitLabUri;

        public String webhookUrl;

        public boolean insecureTls;

    }
}
