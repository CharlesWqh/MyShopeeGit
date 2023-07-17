package com.shopee.shopeegit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.repo.GitRepositoryManager;
import git4idea.actions.GitPull;

public class PullAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        var repositoryManager = GitRepositoryManager.getInstance(project);
        Messages.showInputDialog(
                project,
                "What is your name?",
                "Input your name",
                Messages.getQuestionIcon());
    }
}
