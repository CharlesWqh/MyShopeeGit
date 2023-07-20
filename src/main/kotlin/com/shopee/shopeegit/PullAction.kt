package com.shopee.shopeegit

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitNotificationIdsHolder
import git4idea.GitVcs
import git4idea.actions.GitRepositoryAction
import git4idea.branch.GitBranchUtil
import git4idea.branch.GitBrancher
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitBranchTrackInfo
import git4idea.repo.GitRepositoryManager

class PullActionKt : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val vcs = GitVcs.getInstance(e.project!!)
        val roots = GitRepositoryAction.getGitRoots(e.project!!, vcs)
        if (roots.isNullOrEmpty()) return
        val selectedRepo = GitBranchUtil.guessRepositoryForOperation(e.project!!, e.dataContext)
        val defaultRoot = selectedRepo?.root ?: roots[0]
        val repoManager = GitRepositoryManager.getInstance(e.project!!)
        val repository = repoManager.getRepositoryForFileQuick(defaultRoot)
        if (repository != null) {
            GitVcs.runInBackground(object : Task.Backgroundable(e.project!!, MyGitBundle.message("branches.pull.process"), true) {
                private val failPulled = arrayListOf<String>()

                override fun run(indicator: ProgressIndicator) {
                    val fetchSupport = GitFetchSupport.fetchSupport(project)
                    fetchSupport.fetchAllRemotes(arrayListOf(repository))

                    val currentBranch = repository.currentBranch?.name
                    val pullBranchNames = arrayListOf<String>()
                    pullBranchNames.add(currentBranch + "_test")
                    pullBranchNames.add(currentBranch + "_uat")
                    pullBranchNames.add(currentBranch + "_master")

                    val brancher = GitBrancher.getInstance(project)
                    var currentBranchTracks: GitBranchTrackInfo? = null
                    for (branchTracks in repository.branchTrackInfos) {
                        val localBranchName = branchTracks.localBranch.name
                        if (pullBranchNames.contains(localBranchName)) {
                            try {
                                brancher.checkout(localBranchName, false, listOf(repository), null)
                                gitPull(project, defaultRoot, branchTracks)
                            }
                            catch (ignored: VcsException) {
                                failPulled.add(localBranchName)
                            }
                        }
                        if (currentBranch == localBranchName) {
                            currentBranchTracks = branchTracks
                        }
                    }
                    if (currentBranchTracks != null) {
                        val localBranchName = currentBranchTracks.localBranch.name
                        try {
                            brancher.checkout(localBranchName, false, listOf(repository), null)
                            gitPull(project, defaultRoot, currentBranchTracks)
                        }
                        catch (ignored: VcsException) {
                            failPulled.add(localBranchName)
                        }
                    }
                }

                override fun onSuccess() {
                    if (failPulled.isNotEmpty()) {
                        VcsNotifier.getInstance(project).notifyError(
                            GitNotificationIdsHolder.PULL_FAILED, "",
                            MyGitBundle.message("branches.pull.failed.title",
                                failPulled.size,
                                failPulled.joinToString("\n")))
                    }
                }
            })
        }
    }

    @Throws(VcsException::class)
    fun gitPull(project: Project, root: VirtualFile, remoteBranchTrackInfo: GitBranchTrackInfo) {
        val urls = remoteBranchTrackInfo.remote.urls
        val h = GitLineHandler(project, root, GitCommand.PULL)
        h.urls = urls
        Git.getInstance().runCommand(h).throwOnError()
    }
}