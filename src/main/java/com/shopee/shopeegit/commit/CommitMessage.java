package com.shopee.shopeegit.commit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author Damien Arrachequesne <damien.arrachequesne@gmail.com>
 */
class CommitMessage {
    public static final String ISSUE_TITLE_FORMAT = "[%s]: ";
    public static final String DESCRIPTION_TITLE_FORMAT = "%s - ";
    public static final Pattern COMMIT_MESSAGE_PATTERN = Pattern.compile("^\\[([\\w-]+)\\]: (\\w+) - (.+)$");
    private ChangeType changeType;
    private String longDescription, closedIssues;

    private CommitMessage() {
        this.longDescription = "";
        this.closedIssues = "";
    }

    public CommitMessage(ChangeType changeType, String longDescription, String closedIssues) {
        this.changeType = changeType;
        this.longDescription = longDescription;
        this.closedIssues = closedIssues;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (isNotBlank(closedIssues)) {
            builder
                    .append(String.format(ISSUE_TITLE_FORMAT, formatClosedIssue(closedIssues)));
        }

        if (isNotBlank(longDescription)) {
            builder
                    .append(String.format(DESCRIPTION_TITLE_FORMAT, changeType.label()))
                    .append(formatDescription(longDescription));
        }

        return builder.toString();
    }

    private String formatDescription(String longDescription) {
        String trimmed = longDescription.trim();
        Pattern p1 = Pattern.compile("(\r?\n(\\s)+)");
        return p1.matcher(trimmed).replaceAll(",");
    }

    private String formatClosedIssue(String closedIssue) {
        String trimmed = closedIssue.trim();
        return upperCase(trimmed);
    }

    public static CommitMessage parse(String message) {
        CommitMessage commitMessage = new CommitMessage();

        try {
            Matcher matcher = COMMIT_MESSAGE_PATTERN.matcher(message);
            if (!matcher.find()) return commitMessage;
            commitMessage.closedIssues = matcher.group(1);
            commitMessage.changeType = ChangeType.valueOf(matcher.group(2).toUpperCase());
            commitMessage.longDescription = matcher.group(3);
        } catch (RuntimeException ignored) {}

        return commitMessage;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public String getClosedIssues() {
        return closedIssues;
    }

    public void setClosedIssues(String s) {
        this.closedIssues = s;
    }
}