package com.shopee.shopeegit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.repo.GitRepositoryManager;
import git4idea.actions.GitPull;
import org.jetbrains.annotations.NotNull;

public class PullAction extends GitPull {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        super.actionPerformed(event);
    }
}
