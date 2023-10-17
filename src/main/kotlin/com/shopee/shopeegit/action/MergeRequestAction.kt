package com.shopee.shopeegit.action

import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.push.*
import com.intellij.history.Label
import com.intellij.history.LocalHistory
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.*
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx
import com.intellij.openapi.vcs.update.AbstractCommonUpdateAction
import com.intellij.openapi.vcs.update.ActionInfo
import com.intellij.openapi.vcs.update.UpdatedFiles
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ModalityUiUtil
import com.intellij.vcs.ViewUpdateInfoNotification
import com.shopee.shopeegit.PushMergeRequestDialog
import com.shopee.shopeegit.Utils
import git4idea.*
import git4idea.actions.GitRepositoryAction
import git4idea.branch.*
import git4idea.commands.*
import git4idea.i18n.GitBundle
import git4idea.merge.*
import git4idea.rebase.GitHandlerRebaseEditorManager
import git4idea.repo.GitRepository
import git4idea.update.*
import git4idea.util.GitUntrackedFilesHelper
import git4idea.util.LocalChangesWouldBeOverwrittenHelper
import java.util.*
import java.util.function.Supplier

class MergeRequestActionKT : VcsPushAction() {
    companion object {
        private const val PUSH_TEST = "Quick Merge Test"
        private const val PUSH_UAT = "Quick Merge Uat"
        private const val PUSH_MASTER = "Quick Merge Master"
    }
    private var currentBranch: GitBranch? = null
    private var targetBranch: GitRemoteBranch? = null
    private var mergeBranch: GitBranch? = null
    private var defaultRepository: GitRepository? = null

    override fun actionPerformed(e: AnActionEvent) {
        val vcs = GitVcs.getInstance(e.project!!)
        val roots = GitRepositoryAction.getGitRoots(e.project!!, vcs)
        if (roots.isNullOrEmpty()) return
        val selectedRepo = GitBranchUtil.guessRepositoryForOperation(e.project!!, e.dataContext)
        val defaultRoot = selectedRepo?.root ?: roots[0]
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(e.project!!, GitBundle.message("branches.checkout")) {
                override fun run(indicator: ProgressIndicator) {
                    val branchWorker = GitBranchWorker(project, Git.getInstance(), GitBranchUiHandlerImpl(project, indicator))
                    // 1.checkout
                    if (mergeBranch == null) {
                        val mergeBranchName = currentBranch!!.name + "_" + targetBranch!!.nameForRemoteOperations
                        branchWorker.checkoutNewBranch(mergeBranchName, listOf(defaultRepository))
                    } else {
                        branchWorker.checkout(mergeBranch!!.name, false, listOf(defaultRepository))
                    }
                }

                override fun onFinished() {
                    ProgressManager.getInstance()
                        .run(object : Backgroundable(project, GitBundle.message("rebase.progress.indicator.title")) {
                            override fun run(indicator: ProgressIndicator) {
                                val branchWorker = GitBranchWorker(project, Git.getInstance(), GitBranchUiHandlerImpl(project, indicator))
                                // 2.merge target branch
                                branchWorker.merge(targetBranch!!.name, GitBrancher.DeleteOnMergeOption.NOTHING, listOf(defaultRepository))
                            }

                            override fun onFinished() {
                                if (!defaultRepository!!.isRebaseInProgress) {
                                    // 3.merge feature branch
                                    mergePerform(project, defaultRoot)
                                }
                            }
                        })
                }
            })
    }

    private fun mergePerform(project: Project, selectedRoot: VirtualFile) {
        val beforeLabel =
            LocalHistory.getInstance().putSystemLabel(project, GitBundle.message("merge.action.before.update.label"))
        val selectedBranch = currentBranch
        val handlerProvider: Supplier<GitLineHandler> = getHandlerProvider(project, selectedRoot, selectedBranch!!)

        val title = GitBundle.message("merging.title", selectedRoot.path)
        object : Task.Backgroundable(project, title, true) {
            override fun onFinished() {
                PushMergeRequestDialog(project, defaultRepository!!, currentBranch!!.name, mergeBranch!!.name,
                    targetBranch!!.name).show()
            }

            override fun run(indicator: ProgressIndicator) {
                val git = Git.getInstance()
                val localChangesDetector = GitLocalChangesWouldBeOverwrittenDetector(
                    selectedRoot,
                    GitLocalChangesWouldBeOverwrittenDetector.Operation.MERGE
                )
                val untrackedFilesDetector = GitUntrackedFilesOverwrittenByOperationDetector(selectedRoot)
                val mergeConflict = GitSimpleEventDetector(GitSimpleEventDetector.Event.MERGE_CONFLICT)
                val repository = defaultRepository!!
                var updatedRanges: GitUpdatedRanges? = null
                if (repository.currentBranch != null) {
                    val refPair = GitBranchPair(repository.currentBranch!!, selectedBranch)
                    updatedRanges =
                        GitUpdatedRanges.calcInitialPositions(project, Collections.singletonMap(repository, refPair))
                }
                val beforeRevision = repository.currentRevision
                val rebaseEditorManager = Ref.create<GitHandlerRebaseEditorManager>()
                try {
                    DvcsUtil.workingTreeChangeStarted(project, getActionName()).use {
                        val result = git.runCommand {
                            val handler = handlerProvider.get()
                            handler.addLineListener(localChangesDetector)
                            handler.addLineListener(untrackedFilesDetector)
                            handler.addLineListener(mergeConflict)
                            handler
                        }
                        if (beforeRevision != null) {
                            val currentRev = GitRevisionNumber(beforeRevision)
                            handleResult(
                                result,
                                project,
                                mergeConflict,
                                localChangesDetector,
                                untrackedFilesDetector,
                                repository,
                                currentRev,
                                beforeLabel,
                                updatedRanges,
                                true
                            )
                        }
                    }
                } finally {
                    if (!rebaseEditorManager.isNull) {
                        rebaseEditorManager.get().close()
                    }
                }
            }
        }.queue()
    }

    private fun getHandlerProvider(project: Project?, root: VirtualFile, selectedBranch: GitBranch): Supplier<GitLineHandler> {
        return Supplier {
            val h = GitLineHandler(project, root, GitCommand.MERGE)
            h.addParameters(selectedBranch.name)
            h
        }
    }

    private fun handleResult(
        result: GitCommandResult,
        project: Project,
        mergeConflictDetector: GitSimpleEventDetector,
        localChangesDetector: GitLocalChangesWouldBeOverwrittenDetector,
        untrackedFilesDetector: GitUntrackedFilesOverwrittenByOperationDetector,
        repository: GitRepository,
        currentRev: GitRevisionNumber,
        beforeLabel: Label,
        updatedRanges: GitUpdatedRanges?,
        commitAfterMerge: Boolean
    ) {
        val root = repository.root
        if (mergeConflictDetector.hasHappened()) {
            val merger = GitMerger(project)
            object : GitConflictResolver(project, listOf(root), Params(project)) {
                @Throws(VcsException::class)
                override fun proceedAfterAllMerged(): Boolean {
                    if (commitAfterMerge) {
                        merger.mergeCommit(root)
                    }
                    return true
                }
            }.merge()
        }
        if (result.success() || mergeConflictDetector.hasHappened()) {
            GitUtil.refreshVfsInRoot(root)
            repository.update()
            if (updatedRanges != null &&
                AbstractCommonUpdateAction.showsCustomNotification(listOf(GitVcs.getInstance(project))) &&
                commitAfterMerge
            ) {
                val ranges = updatedRanges.calcCurrentPositions()
                val notificationData: GitUpdateInfoAsLog.NotificationData? =
                    GitUpdateInfoAsLog(project, ranges).calculateDataAndCreateLogTab()
                val notification: Notification = if (notificationData != null) {
                    val title = getTitleForUpdateNotification(
                        notificationData.updatedFilesCount,
                        notificationData.receivedCommitsCount
                    )
                    val content = getBodyForUpdateNotification(notificationData.filteredCommitsCount)
                    VcsNotifier.STANDARD_NOTIFICATION
                        .createNotification(title, content, NotificationType.INFORMATION)
                        .setDisplayId(GitNotificationIdsHolder.FILES_UPDATED_AFTER_MERGE)
                        .addAction(
                            NotificationAction.createSimple(
                                GitBundle.message("action.NotificationAction.GitMergeAction.text.view.commits"),
                                notificationData.viewCommitAction
                            )
                        )
                } else {
                    VcsNotifier.STANDARD_NOTIFICATION
                        .createNotification(
                            VcsBundle.message("message.text.all.files.are.up.to.date"),
                            NotificationType.INFORMATION
                        )
                        .setDisplayId(GitNotificationIdsHolder.FILES_UP_TO_DATE)
                }
                VcsNotifier.getInstance(project).notify(notification)
            } else {
                showUpdates(project, repository, currentRev, beforeLabel, getActionName())
            }
        } else if (localChangesDetector.wasMessageDetected()) {
            LocalChangesWouldBeOverwrittenHelper.showErrorNotification(
                project, GitNotificationIdsHolder.LOCAL_CHANGES_DETECTED, repository.root, getActionName(),
                localChangesDetector.relativeFilePaths
            )
        } else if (untrackedFilesDetector.wasMessageDetected()) {
            GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(
                project, root, untrackedFilesDetector.relativeFilePaths,
                getActionName(), null
            )
        } else {
            VcsNotifier.getInstance(project)
                .notifyError(
                    getNotificationErrorDisplayId(),
                    GitBundle.message("merge.action.operation.failed", getActionName()),
                    result.errorOutputAsHtmlString
                )
            repository.update()
        }
    }

    private fun showUpdates(
        project: Project,
        repository: GitRepository,
        currentRev: GitRevisionNumber,
        beforeLabel: Label,
        actionName: @NlsActions.ActionText String
    ) {
        try {
            val files = UpdatedFiles.create()
            val collector = MergeChangeCollector(project, repository, currentRev)
            collector.collect(files)
            ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState()) {
                val manager =
                    ProjectLevelVcsManager.getInstance(project) as ProjectLevelVcsManagerEx
                val tree = manager.showUpdateProjectInfo(files, actionName, ActionInfo.UPDATE, false)
                if (tree != null) {
                    tree.setBefore(beforeLabel)
                    tree.setAfter(
                        LocalHistory.getInstance()
                            .putSystemLabel(project, GitBundle.message("merge.action.after.update.label"))
                    )
                    ViewUpdateInfoNotification.focusUpdateInfoTree(project, tree)
                }
            }
        } catch (e: VcsException) {
            GitVcs.getInstance(project).showErrors(listOf(e), actionName)
        }
    }

    private fun getNotificationErrorDisplayId(): String {
        return GitNotificationIdsHolder.MERGE_FAILED
    }

    private fun getActionName(): String {
        return GitBundle.message("merge.action.name")
    }

    override fun update(e: AnActionEvent) {
        var targetBranchName = ""
        when (e.presentation.text) {
            PUSH_TEST -> {
                targetBranchName = "origin/test"
            }
            PUSH_UAT -> {
                targetBranchName = "origin/uat"
            }
            PUSH_MASTER -> {
                targetBranchName = "origin/master"
            }
        }
        defaultRepository = Utils.getDefaultGitRepository(e)
        targetBranch = defaultRepository!!.branches.findRemoteBranch(targetBranchName)
        currentBranch = defaultRepository!!.branches.findBranchByName(defaultRepository!!.currentBranch!!.name)

        if (currentBranch != null) {
            mergeBranch = defaultRepository!!.branches.findBranchByName(currentBranch!!.name +
                    "_" + targetBranch!!.nameForRemoteOperations)
        }
        var isEnable = false
        if (targetBranch != null) {
            isEnable = true
        }

        e.presentation.isEnabled = isEnable
    }
}