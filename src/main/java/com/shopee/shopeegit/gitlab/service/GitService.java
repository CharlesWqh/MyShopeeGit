package com.shopee.shopeegit.gitlab.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.shopee.shopeegit.gitlab.exception.SourceAndTargetBranchCannotBeEqualException;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
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

    public Collection<GitLocalBranch> getLocalBranches(Project project, VirtualFile file) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        GitRepository repo = repositoryManager.getRepositoryForFile(file);
        if (repo == null) {
            return null;
        }
        GitBranchesCollection branches = repo.getBranches();
        return branches.getLocalBranches();
    }

    public String getSourceBranch() {
        return sourceBranchName;
    }

    public boolean isRemoteBranchExist(String branchName) {
        GitRemoteBranch branch = currentRepo.getBranches().findRemoteBranch(branchName);
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

    public static final Pattern pattern = Pattern.compile("(?:git|ssh|https?|git@[-\\w.]+):(//)?(.*?)(\\.git)(/?|#[-\\d\\w._]+?)$");

    private String getRepoPathWithoutDotGit(String url) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String repoId = matcher.group(2);
            if (url.startsWith("http")) {
                // Let's extract away hostname.
                // E. g. gitlab.com/example/dummy-project is transformed to
                // example/dummy-project
                repoId = repoId.substring(repoId.indexOf("/") + 1);
            }
            return repoId;
        }
        return null;
    }

    public Optional<String> getLastCommitMessage(Project project) {
        try {
            return Optional.ofNullable(GitHistoryUtils.getCurrentRevisionDescription(project, VcsUtil
                    .getFilePath(project.getBaseDir())))
                    .map(VcsRevisionDescription::getCommitMessage);
        } catch (VcsException e) {
            LOG.error("Unable to load last commit message", e);
            return Optional.empty();
        }
    }

    public Optional<String> getLastCommitMessageSubject(Project project) {
        return getLastCommitMessage(project)
                .filter(commitMessage -> commitMessage.contains(System.lineSeparator()))
                .map(commitMessage -> commitMessage.substring(0, commitMessage.indexOf(System.lineSeparator())).trim());
    }

    public Optional<String> getLastCommitMessageBody(Project project) {
        return getLastCommitMessage(project)
                .filter(commitMessage -> commitMessage.contains(System.lineSeparator()))
                .map(commitMessage -> commitMessage.substring(commitMessage.indexOf(System.lineSeparator())).trim());
    }
}
