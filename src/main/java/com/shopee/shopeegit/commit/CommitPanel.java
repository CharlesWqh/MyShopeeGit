package com.shopee.shopeegit.commit;

import javax.swing.*;
import java.util.Enumeration;

/**
 * @author Damien Arrachequesne
 */
public class CommitPanel {
    private JPanel mainPanel;
    private JTextArea longDescription;
    private JTextField closedIssues;
    private ButtonGroup changeTypeGroup;
    private JRadioButton featRadioButton;
    private JRadioButton fixRadioButton;
    private JRadioButton ciRadioButton;

    CommitPanel(CommitMessage commitMessage) {
        if (commitMessage != null) {
            restoreValuesFromParsedCommitMessage(commitMessage);
        }
    }

    JPanel getMainPanel() {
        return mainPanel;
    }

    CommitMessage getCommitMessage() {
        return new CommitMessage(
                getSelectedChangeType(),
                longDescription.getText().trim(),
                closedIssues.getText().trim()
        );
    }

    private ChangeType getSelectedChangeType() {
        for (Enumeration<AbstractButton> buttons = changeTypeGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                return ChangeType.valueOf(button.getActionCommand().toUpperCase());
            }
        }
        return null;
    }

    private void restoreValuesFromParsedCommitMessage(CommitMessage commitMessage) {
        if (commitMessage.getChangeType() != null) {
            for (Enumeration<AbstractButton> buttons = changeTypeGroup.getElements(); buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();

                if (button.getActionCommand().equalsIgnoreCase(commitMessage.getChangeType().label())) {
                    button.setSelected(true);
                }
            }
        }
        longDescription.setText(commitMessage.getLongDescription());
        closedIssues.setText(commitMessage.getClosedIssues());
    }
}
