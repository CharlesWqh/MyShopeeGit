package com.shopee.shopeegit.action

import com.intellij.dvcs.getCommonCurrentBranch
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.shopee.shopeegit.MyGitBundle
import com.shopee.shopeegit.NewBranchDialogue
import com.shopee.shopeegit.Utils
import git4idea.GitNotificationIdsHolder
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.branch.GitBrancher
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepository

class NewBranchActionKt
    : DumbAwareAction(DvcsBundle.messagePointer("new.branch.action.text.with.ellipsis"),
    DvcsBundle.messagePointer("new.branch.action.description"),
    AllIcons.General.Add) {

    override fun actionPerformed(e: AnActionEvent) {
        val repository = Utils.getDefaultGitRepository(e)
        val repositories = arrayListOf<GitRepository>()
        repositories.add(repository!!)
        val currentBranch = repositories.getCommonCurrentBranch()
        val options = NewBranchDialogue(e.project!!, repositories, GitUtil.HEAD, currentBranch).showAndGetOptions()
        if (options != null) {
            val brancher = GitBrancher.getInstance(e.project!!)
            val fetchSupport = GitFetchSupport.fetchSupport(e.project!!)
            brancher.checkoutNewBranchStartingFrom(options.name, GitUtil.HEAD, repositories, null)

            GitVcs.runInBackground(object : Task.Backgroundable(e.project!!,
                MyGitBundle.message("branches.creating.process"), true) {
                private val successfullyUpdated = arrayListOf<String>()

                override fun run(indicator: ProgressIndicator) {
                    val remoteBranchNames = arrayListOf<String>()
                    if (options.test) {
                        remoteBranchNames.add("test")
                    }
                    if (options.uat) {
                        remoteBranchNames.add("uat")
                    }
                    if (options.master) {
                        remoteBranchNames.add("master")
                    }
                    if (remoteBranchNames.isNotEmpty()) {
                        for (remoteBranch in repository.info.remoteBranchesWithHashes) {
                            val remoteBranchName = remoteBranch.key.nameForRemoteOperations
                            if (remoteBranchNames.contains(remoteBranchName)) {
                                val localBranchName = options.name + "_" + remoteBranchName
                                val fetchResult = fetchSupport.fetch(repository, remoteBranch.key.remote,
                                    "$remoteBranchName:$localBranchName")

                                try {
                                    fetchResult.throwExceptionIfFailed()
                                    successfullyUpdated.add(localBranchName)
                                }
                                catch (ignored: VcsException) {
                                    fetchResult.showNotificationIfFailed(MyGitBundle.message("branches.create.failed"))
                                }
                            }
                        }
                    }
                }

                override fun onSuccess() {
                    if (successfullyUpdated.isNotEmpty()) {
                        VcsNotifier.getInstance(project).notifySuccess(
                            GitNotificationIdsHolder.BRANCHES_UPDATE_SUCCESSFUL, "",
                            MyGitBundle.message(
                                "branches.created.title",
                                successfullyUpdated.size,
                                successfullyUpdated.joinToString("\n")
                            )
                        )
                    }
                }
            })
        }
    }
}