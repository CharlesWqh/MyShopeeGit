package com.shopee.shopeegit.commit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitVcs;
import git4idea.actions.GitRepositoryAction;
import git4idea.branch.GitBranchUtil;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author Damien Arrachequesne
 */
public class CommitPanel {
    private JPanel mainPanel;
    private JTextArea longDescription;
    private JTextField closedIssues;

    CommitPanel(@NotNull Project project, CommitMessage commitMessage) {
        if (commitMessage != null) {
            restoreValuesFromParsedCommitMessage(commitMessage);
        }
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    CommitMessage getCommitMessage() {
        return new CommitMessage(
                longDescription.getText().trim(),
                closedIssues.getText().trim()
        );
    }

    private void restoreValuesFromParsedCommitMessage(CommitMessage commitMessage) {
        longDescription.setText(commitMessage.getLongDescription());
        closedIssues.setText(commitMessage.getClosedIssues());
    }
}
