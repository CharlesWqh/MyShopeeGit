package com.shopee.shopeegit.gitlab.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import git4idea.GitRemoteBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitService {
    private static final Logger LOG = Logger.getInstance("#com.github.novotnyr.idea.git.GitService");

    private final Project myProject;

    private final String sourceBranchName;

    private final GitRepository currentRepo;

    public GitService(Project myProject, String sourceBranchName, GitRepository currentRepo) {
        this.myProject = myProject;
        this.sourceBranchName = sourceBranchName;
        this.currentRepo = currentRepo;
    }

    public String getSourceBranch() {
        return sourceBranchName;
    }

    public boolean isRemoteBranchExist(String branchName) {
        GitRemoteBranch branch = currentRepo.getBranches().findRemoteBranch("origin/" + branchName);
        return branch != null;
    }

    public String getGitLabProjectId() {
        String projectGitUrl = getProjectGitUrl();
        if (projectGitUrl == null) {
            return "";
        }
        return getRepoPathWithoutDotGit(projectGitUrl);
    }

    private String getProjectGitUrl() {
        for (GitRemote remote : currentRepo.getRemotes()) {
            return remote.getFirstUrl();
        }
        return null;
    }

    public static final Pattern pattern = Pattern.compile("(?:git|ssh|https?|gitlab@[-\\w.]+):(//)?(.*?)(\\.git)(/?|#[-\\d\\w._]+?)$");

    private String getRepoPathWithoutDotGit(String url) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String repoId = matcher.group(2);
            if (url.startsWith("http")) {
                repoId = repoId.substring(repoId.indexOf("/") + 1);
            }
            return repoId;
        }
        return null;
    }
}
