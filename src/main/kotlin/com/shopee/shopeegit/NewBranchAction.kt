package com.shopee.shopeegit

import com.intellij.dvcs.getCommonCurrentBranch
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import git4idea.GitUtil
import git4idea.actions.branch.GitBranchActionsUtil.getRepositoriesForTopLevelActions
import git4idea.actions.branch.GitNewBranchAction
import git4idea.branch.GitBrancher
import git4idea.repo.GitRepository

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
            create(repositories, brancher, GitUtil.HEAD, options.name, false)
            if (options.test) {
                brancher.checkout("origin/test", false, repositories, null)
                create(repositories, brancher, GitUtil.HEAD, options.name + "_test", false)
            }
            if (options.uat) {
                brancher.checkout("origin/uat", false, repositories, null)
                create(repositories, brancher, GitUtil.HEAD, options.name + "_uat", false)
            }
            if (options.master) {
                brancher.checkout("origin/master", false, repositories, null)
                create(repositories, brancher, GitUtil.HEAD, options.name + "_master", false)
            }
            brancher.checkout(options.name, false, repositories, null)
        }
    }

    private fun create(repositories: List<GitRepository>, brancher: GitBrancher, startPoint: String, name: String, reset: Boolean) {
        val (reposWithLocalBranch, reposWithoutLocalBranch) = repositories.partition { it.branches.findLocalBranch(name) != null }

        if (reposWithLocalBranch.isNotEmpty() && reset) {
            val (currentBranchOfSameName, currentBranchOfDifferentName) = reposWithLocalBranch.partition { it.currentBranchName == name }
            //git checkout -B for current branches with the same name (cannot force update current branch) and git branch -f for not current
            if (currentBranchOfSameName.isNotEmpty()) {
                brancher.checkoutNewBranchStartingFrom(name, startPoint, true, currentBranchOfSameName, null)
            }
            if (currentBranchOfDifferentName.isNotEmpty()) {
                brancher.createBranch(name, currentBranchOfDifferentName.associateWith { startPoint }, true)
            }
        }

        if (reposWithoutLocalBranch.isNotEmpty()) {
            brancher.createBranch(name, reposWithoutLocalBranch.associateWith { startPoint })
        }
    }
}