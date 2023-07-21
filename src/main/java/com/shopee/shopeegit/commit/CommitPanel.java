package com.shopee.shopeegit.commit;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
