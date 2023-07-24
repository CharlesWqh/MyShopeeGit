package com.shopee.shopeegit.commit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author Damien Arrachequesne <damien.arrachequesne@gmail.com>
 */
class CommitMessage {
    public static final String ISSUE_TITLE_FORMAT = "[IssueNo]:";
    public static final String DESCRIPTION_TITLE_FORMAT = "[%s] :";
    public static final Pattern CHANGE_TYPE_PATTERN = Pattern.compile("^\\[([a-z]+)\\] :(.+)");
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
                    .append(ISSUE_TITLE_FORMAT)
                    .append(formatClosedIssue(closedIssues));
        }

        if (isNotBlank(longDescription)) {
            builder
                    .append(System.lineSeparator())
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
            String[] strings = message.split(System.lineSeparator());
            if (strings.length == 0) return commitMessage;
            for (String lineString : strings) {
                int issueIndex = lineString.indexOf(ISSUE_TITLE_FORMAT);
                if (issueIndex >= 0) {
                    commitMessage.closedIssues = lineString.substring(issueIndex + length(ISSUE_TITLE_FORMAT));
                } else {
                    Matcher matcher = CHANGE_TYPE_PATTERN.matcher(lineString);
                    if (!matcher.find()) return commitMessage;
                    commitMessage.changeType = ChangeType.valueOf(matcher.group(1).toUpperCase());
                    commitMessage.longDescription = matcher.group(2);
                }
            }

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