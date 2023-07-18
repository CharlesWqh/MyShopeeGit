package com.shopee.shopeegit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.actions.branch.GitNewBranchAction;
import org.jetbrains.annotations.NotNull;


public class NewBranchAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        NewBranchActionKt action = new NewBranchActionKt();
        action.actionPerformed(event);
    }
}
