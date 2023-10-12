package com.shopee.shopeegit.gitlab.settings;

import com.shopee.shopeegit.gitlab.GitLab;
import com.shopee.shopeegit.gitlab.GitLabHttpResponseBody;
import com.shopee.shopeegit.gitlab.exception.AccessDeniedException;
import com.shopee.shopeegit.gitlab.exception.IllegalGitLabUrlException;
import com.google.gson.JsonSyntaxException;
import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class SettingsUi implements Configurable {
    private final Project project;
    private final BrowserLauncher browserLauncher;

    private JBTextField urlTextField;
    private JBPasswordField accessTokenTextField;
    private JBTextField targetBranchTextField;
    private JButton validateServerButton;
    private JPanel rootPanel;
    private JButton openAccessTokenUrlButton;
    private JCheckBox insecureTLSCheckBox;
    private Settings settings;

    private boolean serverUrlValidated = true;

    /**
     * Cached hashcode of access token to speed up isModified()
     */
    private int accessTokenHashCode;

    public SettingsUi(Project project) {
        this.project = project;
        this.browserLauncher = BrowserLauncher.getInstance();

        this.urlTextField.getEmptyText().setText("https://gitlab.com/api/v4");

        this.openAccessTokenUrlButton.setIcon(AllIcons.General.Web);

        this.validateServerButton.addActionListener(this::onValidateServerButtonClicked);

        this.openAccessTokenUrlButton.addActionListener(this::onOpenAccessTokenUrlButtonClicked);
    }

    private void bindToComponents(Settings settings) {
        this.urlTextField.setText(settings.getGitLabUri());
        this.accessTokenTextField.setText(settings.getAccessToken());
        this.targetBranchTextField.setText(settings.getDefaultTargetBranch());
        this.insecureTLSCheckBox.setSelected(settings.isInsecureTls());
    }

    private void onValidateServerButtonClicked(ActionEvent event) {
        if (StringUtils.isEmpty(this.urlTextField.getText())) {
            warnInvalidServer(new IllegalGitLabUrlException("GitLab URL cannot be empty"));
            return;
        }

        GitLab gitLab = new GitLab(this.urlTextField.getText(), String.valueOf(accessTokenTextField.getPassword()), insecureTLSCheckBox.isSelected());
                gitLab.version().thenRun(() -> {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("GitLab connection successful", MessageType.INFO, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(this.validateServerButton),
                                    Balloon.Position.atRight);

                })
                .exceptionally(t -> {
                    warnInvalidServer(t);
                    return null;
                });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void onOpenAccessTokenUrlButtonClicked(ActionEvent e) {
        String url = this.urlTextField.getText();
        String baseUrl = GitLab.getBaseUrl(url);
        String accessTokenUrl = GitLab.getAccessTokenWebPageUrl(baseUrl);
        this.browserLauncher.browse(URI.create(accessTokenUrl));
    }

    public List<String> validate() {
        List<String> validationErrors = new ArrayList<>();
        if (!StringUtils.isNotEmpty(this.urlTextField.getText())) {
            validationErrors.add("Missing GitLab URI");
        }
        if (!StringUtils.isNotEmpty(this.targetBranchTextField.getText())) {
            validationErrors.add("Missing default target branch");
        }
        return validationErrors;
    }

    private void warnInvalidServer(Throwable throwable) {
        String errorMessage = getInvalidServerErrorMessage(throwable);
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(errorMessage, MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(this.validateServerButton),
                        Balloon.Position.atRight);
    }

    private String getInvalidServerErrorMessage(Throwable throwable) {
        String defaultErrorMessage = "GitLab is not available. Please check URL or access token.";
        StringBuilder additionalErrorMessage = new StringBuilder("\n");
        Throwable cause = throwable;
        if (throwable instanceof CompletionException) {
            cause = throwable.getCause();
        }
        if (cause instanceof SSLPeerUnverifiedException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append("SSL/TLS certificate is not valid.\nIf you are using a self-signed TLS certificate on GitLab, please check the 'Insecure TLS' checkbox");
        }

        if (cause instanceof IllegalGitLabUrlException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append(cause.getMessage());
        }
        if (cause instanceof JsonSyntaxException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append("This is not a valid GitLab V4 REST API URL\nServer URL must end with /api/v4. Example: http://gitlab.com/api/v4");
        }
        if (cause instanceof HttpResponseException) {
            HttpResponseException httpResponseException = (HttpResponseException) cause;
            additionalErrorMessage
                    .append("HTTP Status: ").append(httpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(httpResponseException.getMessage());
        }
        if (cause instanceof AccessDeniedException.GitLabHttpResponseException) {
            AccessDeniedException.GitLabHttpResponseException gitLabHttpResponseException = (AccessDeniedException.GitLabHttpResponseException) cause;
            GitLabHttpResponseBody responseBody = gitLabHttpResponseException.getResponseBody();
            if (gitLabHttpResponseException.getStatusCode() == 404) {
                defaultErrorMessage = "";
                additionalErrorMessage.append("GitLab V4 REST API not found in this URL\n");
            }
            if (gitLabHttpResponseException.getStatusCode() == 503) {
                defaultErrorMessage = "";
                if (responseBody.containsCaseInsensitive("checking your browser")) {
                    additionalErrorMessage.append("GitLab V4 REST API not found in this URL\n");
                }
            }
            additionalErrorMessage
                    .append("HTTP Status: ").append(gitLabHttpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(gitLabHttpResponseException.getMessage()).append("\n")
                    .append("HTTP Response: ").append(responseBody.asHtml());
        }
        String fullErrorMessage = defaultErrorMessage + additionalErrorMessage;
        if (fullErrorMessage.startsWith("\n")) {
            fullErrorMessage = fullErrorMessage.substring(1);
        }
        return fullErrorMessage;
    }

    //-------

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitLab Quick Merge Request";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return this.rootPanel;
    }

    @Override
    public boolean isModified() {
        boolean unmodified = SettingUtils.equals(this.urlTextField, settings.getGitLabUri())
                && !isAccessTokenModified()
                && SettingUtils.equals(this.targetBranchTextField, settings.getDefaultTargetBranch())
                && this.insecureTLSCheckBox.isSelected() == (settings.isInsecureTls())
                ;

        return !unmodified;
    }

    private boolean isAccessTokenModified() {
        int accessTokenHash = new String(this.accessTokenTextField.getPassword()).hashCode();
        String storedAccessToken = settings.getAccessToken();
        if (storedAccessToken == null) {
            storedAccessToken = "";
        }
        int storedAccessTokenHash = storedAccessToken.hashCode();
        return accessTokenHash != storedAccessTokenHash;
    }

    @Override
    public void reset() {
        this.settings = this.project.getService(Settings.class);
        bindToComponents(settings);
        cacheAccessToken();
    }

    private void cacheAccessToken() {
        String accessToken = this.settings.getAccessToken();
        if (accessToken == null) {
            accessToken = "";
        }
        this.accessTokenHashCode = accessToken.hashCode();
    }

    @Override
    public void apply() throws ConfigurationException {
        List<String> validationErrors = validate();
        if (!validationErrors.isEmpty()) {
            throw new ConfigurationException("<li>" + String.join("<li>", validationErrors));
        }

        settings.setGitLabUri(this.urlTextField.getText());
        settings.setAccessToken(String.valueOf(this.accessTokenTextField.getPassword()));
        settings.setDefaultTargetBranch(this.targetBranchTextField.getText());
        settings.setInsecureTls(this.insecureTLSCheckBox.isSelected());
    }

    public static class ConfigurableProvider implements VcsConfigurableProvider {
        @Override
        public Configurable getConfigurable(Project project) {
            return new SettingsUi(project);
        }
    }
}
