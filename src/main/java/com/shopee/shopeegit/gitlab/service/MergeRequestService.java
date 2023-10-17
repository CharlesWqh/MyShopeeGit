package com.shopee.shopeegit.gitlab.service;

import com.intellij.openapi.project.Project;
import com.shopee.shopeegit.gitlab.GitLab;
import com.shopee.shopeegit.gitlab.MergeRequestRequest;
import com.shopee.shopeegit.gitlab.MergeRequestResponse;
import com.shopee.shopeegit.gitlab.exception.SettingsNotInitializedException;
import com.shopee.shopeegit.gitlab.exception.SourceAndTargetBranchCannotBeEqualException;
import com.shopee.shopeegit.gitlab.settings.Settings;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MergeRequestService {
    private final GitService gitService;

    private final String targetBranch;

    private final Project myProject;

    public MergeRequestService(String sourceBranch, String targetBranch, Project myProject, GitRepository currentRepo) {
        this.gitService = new GitService(myProject, sourceBranch, currentRepo);
        this.targetBranch = targetBranch;
        this.myProject = myProject;
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
        request.setTitle(getTitle());
        request.setDescription(getDescription());
        request.setRemoveSourceBranch(false);
        return request;
    }

    public CompletableFuture<MergeRequestResponse> submit(String gitLabProjectId, MergeRequestRequest mergeRequestRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException {
        GitLab gitLab = createGitLab(settings);
        return gitLab.createMergeRequest(gitLabProjectId, mergeRequestRequest);
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest() throws SourceAndTargetBranchCannotBeEqualException, SettingsNotInitializedException {
        Settings settings = myProject.getService(Settings.class);
        MergeRequestRequest request = prepare(settings);
        return submit(gitService.getGitLabProjectId(), request, settings);
    }

    private String getTitle() {
        String subject = gitService.getLastCommitMessageSubject(myProject).get();
        return subject;
    }

    private String getDescription() {
        String description = gitService.getLastCommitMessageBody(myProject).get();
        return description;
    }


    @NotNull
    protected GitLab createGitLab(Settings settings) {
        return new GitLab(settings.getGitLabUri(), settings.getAccessToken(), settings.isInsecureTls());
    }
}
