package com.shopee.shopeegit;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.branch.GitBranchUiHandlerImpl;
import git4idea.branch.GitBranchWorker;
import git4idea.commands.*;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PushMergeRequestDialog extends com.intellij.dvcs.push.ui.VcsPushDialog {
    private final String sourceBranchName;

    private final GitRepository currentRepo;

    public PushMergeRequestDialog(@NotNull Project project, @NotNull GitRepository currentRepo, @NotNull String branchName) {
        super(project, List.of(currentRepo), currentRepo);
        this.sourceBranchName = branchName;
        this.currentRepo = currentRepo;
    }

    @Override
    public void push(boolean forcePush) {
        super.push(forcePush);
        ProgressManager.getInstance().run(new Task.Backgroundable(myProject, GitBundle.message("branches.checkout")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (myProject == null) {
                    return;
                }
                int i = 0;
                while (!isOK() && i < 10) {
                    i++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                GitBranchWorker branchWorker = new GitBranchWorker(myProject, Git.getInstance(), new GitBranchUiHandlerImpl(myProject, indicator));
                branchWorker.checkout(sourceBranchName, false, List.of(currentRepo));
            }
        });
    }
}
