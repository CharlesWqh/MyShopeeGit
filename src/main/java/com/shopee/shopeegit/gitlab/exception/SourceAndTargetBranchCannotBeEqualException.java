package com.shopee.shopeegit.gitlab.exception;

public class SourceAndTargetBranchCannotBeEqualException extends RuntimeException {

    public SourceAndTargetBranchCannotBeEqualException(String branchName) {
        super("Source branch (" + branchName + ") and target branch must be different");
    }
}