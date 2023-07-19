package com.shopee.shopeegit

import com.intellij.dvcs.getCommonCurrentBranch
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import git4idea.GitNotificationIdsHolder
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.actions.branch.GitBranchActionsUtil.getRepositoriesForTopLevelActions
import git4idea.branch.GitBrancher
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRepositoryManager

class NewBranchActionKt
    : DumbAwareAction(DvcsBundle.messagePointer("new.branch.action.text.with.ellipsis"),
    DvcsBundle.messagePointer("new.branch.action.description"),
    AllIcons.General.Add) {

    private val TOP_LEVEL_ACTION_PLACE = ActionPlaces.getPopupPlace("GitBranchesPopup.TopLevel.Branch.Actions")

    override fun actionPerformed(e: AnActionEvent) {
        val repositories = getRepositoriesForTopLevelActions(e) { it.place == TOP_LEVEL_ACTION_PLACE }
        val currentBranch = repositories.getCommonCurrentBranch()
        val options = NewBranchDialogue(e.project!!, repositories, GitUtil.HEAD, currentBranch).showAndGetOptions()
        if (options != null) {
            val brancher = GitBrancher.getInstance(e.project!!)
            val fetchSupport = GitFetchSupport.fetchSupport(e.project!!)
            brancher.checkoutNewBranchStartingFrom(options.name, GitUtil.HEAD, repositories, null)

            GitVcs.runInBackground(object : Task.Backgroundable(e.project!!, MyGitBundle.message("branches.creating.process"), true) {
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
                        val repoManager = GitRepositoryManager.getInstance(e.project!!)
                        val repository = repoManager.getRepositoryForFileQuick(e.project!!.workspaceFile)
                        if (repository != null) {
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
                }

                override fun onSuccess() {
                    if (successfullyUpdated.isNotEmpty()) {
                        VcsNotifier.getInstance(project).notifySuccess(
                            GitNotificationIdsHolder.BRANCHES_UPDATE_SUCCESSFUL, "",
                            MyGitBundle.message("branches.created.title",
                                successfullyUpdated.size,
                                successfullyUpdated.joinToString("\n")))
                    }
                }
            })
        }
    }
}