package com.shopee.shopeegit;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitVcs;
import git4idea.actions.GitRepositoryAction;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
    private Utils() {}

    public static GitRepository getDefaultGitRepository(@NotNull AnActionEvent actionEvent) {
        Project project = actionEvent.getProject();
        if (project != null) {
            GitVcs vcs = GitVcs.getInstance(project);
            List<VirtualFile> roots = GitRepositoryAction.getGitRoots(project, vcs);
            if (CollectionUtils.isNotEmpty(roots)) {
                GitRepository selectedRepo = GitBranchUtil.getCurrentRepository(project);
                VirtualFile defaultRoot = roots.get(0);
                if (selectedRepo != null) {
                    defaultRoot = selectedRepo.getRoot();
                }
                GitRepositoryManager repoManager = GitRepositoryManager.getInstance(project);
                return repoManager.getRepositoryForFileQuick(defaultRoot);
            }
        }
        return null;
    }

    public static final Pattern jiraPattern = Pattern.compile("([a-zA-Z]+-[0-9]+)");
    public static String getJiraTicketByPattern(String srcName){
        Matcher matcher = jiraPattern.matcher(srcName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return srcName;
    }
}
