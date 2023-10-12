package com.shopee.shopeegit.gitlab.exception;

import com.shopee.shopeegit.gitlab.ProjectId;

public class GitLabProjectNotFoundException extends RuntimeException {
    public GitLabProjectNotFoundException(String message) {
        super(message);
    }

    public static GitLabProjectNotFoundException of(ProjectId projectId) {
        return new GitLabProjectNotFoundException("GitLab project '" + projectId + "' not found. Do GitLab URL and Git remote match?");
    }
}