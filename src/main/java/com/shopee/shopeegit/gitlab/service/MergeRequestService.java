package com.shopee.shopeegit.gitlab.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.shopee.shopeegit.Utils;
import com.shopee.shopeegit.gitlab.GitLab;
import com.shopee.shopeegit.gitlab.MergeRequestRequest;
import com.shopee.shopeegit.gitlab.MergeRequestResponse;
import com.shopee.shopeegit.gitlab.exception.SettingsNotInitializedException;
import com.shopee.shopeegit.gitlab.exception.SourceAndTargetBranchCannotBeEqualException;
import com.shopee.shopeegit.gitlab.settings.Settings;
import com.shopee.shopeegit.jira.JiraProxy;
import com.shopee.shopeegit.seatalk.SeaTalk;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.CompletableFuture;

public class MergeRequestService {
    private final GitService gitService;

    private final String featureBranch;

    private final String targetBranch;

    public MergeRequestService(String featureBranch, String sourceBranch, String targetBranch, Project myProject, GitRepository currentRepo) {
        this.gitService = new GitService(myProject, sourceBranch, currentRepo);
        this.featureBranch = featureBranch;
        this.targetBranch = targetBranch;
    }

    public MergeRequestRequest prepare(Settings settings) throws SourceAndTargetBranchCannotBeEqualException, SettingsNotInitializedException {
        if (!settings.isInitialized()) {
            throw new SettingsNotInitializedException();
        }

        String sourceBranch = gitService.getSourceBranch();
        if (!gitService.isRemoteBranchExist(targetBranch)) {
            throw new SourceAndTargetBranchCannotBeEqualException(sourceBranch);
        }

        MergeRequestRequest request = new MergeRequestRequest();
        request.setSourceBranch(gitService.getSourceBranch());
        request.setTargetBranch(targetBranch);
        request.setTitle(getTitle(settings));
        request.setRemoveSourceBranch(false);
        return request;
    }

    public void submit(String gitLabProjectId, MergeRequestRequest mergeRequestRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException {
        GitLab gitLab = createGitLab(settings);
        CompletableFuture<MergeRequestResponse> result = gitLab.createMergeRequest(gitLabProjectId, mergeRequestRequest);

        try {
            MergeRequestResponse response = result.get(20, TimeUnit.SECONDS);
            SeaTalk seaTalk = new SeaTalk(settings.getWebhookUrl());
            seaTalk.callWebhook(response.getWebUrl(), mergeRequestRequest.getTitle(), settings.getAssignees());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void createMergeRequest() throws SourceAndTargetBranchCannotBeEqualException, SettingsNotInitializedException {
        Settings settings = ApplicationManager.getApplication().getService(Settings.class);
        MergeRequestRequest request = prepare(settings);
        submit(gitService.getGitLabProjectId(), request, settings);
    }

    private String getTitle(Settings settings) {
        String jiraNo = Utils.getJiraTicketByPattern(featureBranch);
        if (jiraNo.isEmpty()) {
            return String.format("[%s]:%s", featureBranch, "merge " + targetBranch);
        }
        JiraProxy jiraProxy = new JiraProxy(settings.getJiraUsername(), settings.getJiraPassword());
        String summary = jiraProxy.getSummaryByIssueNo(jiraNo);
        if (summary.isEmpty()) {
            summary = "merge " + targetBranch;
        }
        return String.format("[%s]:%s", jiraNo, summary);
    }

    @NotNull
    protected GitLab createGitLab(Settings settings) {
        return new GitLab(settings.getGitLabUri(), settings.getAccessToken(), settings.isInsecureTls());
    }
}
