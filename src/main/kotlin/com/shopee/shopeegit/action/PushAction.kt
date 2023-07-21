package com.shopee.shopeegit.action

import com.intellij.dvcs.getCommonCurrentBranch
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import git4idea.GitNotificationIdsHolder
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.actions.GitRepositoryAction
import git4idea.branch.GitBranchUtil
import git4idea.branch.GitBrancher
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

class PushActionKt
    : AnAction(AllIcons.Vcs.Push) {

    override fun actionPerformed(e: AnActionEvent) {

    }
}