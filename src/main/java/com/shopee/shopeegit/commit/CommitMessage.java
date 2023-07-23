package com.shopee.shopeegit.commit;

import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.*;

/**
 * @author Damien Arrachequesne <damien.arrachequesne@gmail.com>
 */
class CommitMessage {
    public static final String ISSUE_TITLE_FORMAT = "[IssueNo]:";
    public static final String DESCRIPTION_TITLE_FORMAT = "[Description]:";

    private String longDescription, closedIssues;

    private CommitMessage() {
        this.longDescription = "";
        this.closedIssues = "";
    }

    public CommitMessage(String longDescription, String closedIssues) {
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
                    .append(DESCRIPTION_TITLE_FORMAT)
                    .append(formatDescription(longDescription));
        }

        return builder.toString();
    }

    private String formatDescription(String longDescription) {
        String trimmed = longDescription.trim();
        Pattern p1 = Pattern.compile("(\r?\n(\\s)+)");
        String str1 = p1.matcher(trimmed).replaceAll("");
//        Pattern p2 = Pattern.compile("((\\s)+)\r?\n");
//        String str2 = p2.matcher(str1).replaceAll("");
        return str1.replaceAll(System.lineSeparator(), ",");
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
                }
                int descIndex = lineString.indexOf(DESCRIPTION_TITLE_FORMAT);
                if (descIndex >= 0) {
                    commitMessage.longDescription = lineString.substring(descIndex + length(DESCRIPTION_TITLE_FORMAT));
                }
            }

        } catch (RuntimeException ignored) {}

        return commitMessage;
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