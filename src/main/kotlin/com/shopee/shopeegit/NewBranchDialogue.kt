package com.shopee.shopeegit

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor
import com.intellij.util.textCompletion.TextCompletionProviderBase
import com.intellij.util.textCompletion.TextFieldWithCompletion
import git4idea.repo.GitRepository
import git4idea.validators.*
import org.jetbrains.annotations.Nls
import javax.swing.JCheckBox

data class ShopeeGitNewBranchOptions @JvmOverloads constructor(val name: String,
                                                         @get:JvmName("createTest") val test: Boolean = true,
                                                         @get:JvmName("createUat") val uat: Boolean = true,
                                                         @get:JvmName("createMaster") val master: Boolean = false)


enum class GitBranchOperationType(@Nls val text: String, @Nls val description: String = "") {
    CREATE(
        MyGitBundle.message("new.branch.dialog.operation.create.name"),
        MyGitBundle.message("new.branch.dialog.operation.create.description")),
}

class NewBranchDialogue @JvmOverloads constructor(private val project: Project,
                                                            private val repositories: Collection<GitRepository>,
                                                            @NlsContexts.DialogTitle dialogTitle: String,
                                                            initialName: String?,
                                                            operation: GitBranchOperationType = GitBranchOperationType.CREATE
)
    : DialogWrapper(project, true) {

    companion object {
        private const val NAME_SEPARATOR = '/'
    }

    private var test = true
    private var uat = true
    private var master = false
    private var currentBranch = initialName.orEmpty()
    private var branchName = initialName.orEmpty()
    private val validator = GitRefNameValidator.getInstance()

    private val localBranchDirectories = collectDirectories(collectLocalBranchNames().asIterable(), false).toSet()

    init {
        title = dialogTitle
        setOKButtonText(operation.text)
        init()
    }

    fun showAndGetOptions(): ShopeeGitNewBranchOptions? {
        if (!showAndGet()) return null
        return ShopeeGitNewBranchOptions(validator.cleanUpBranchName(branchName).trim(), test, uat, master)
    }

    override fun createCenterPanel() = panel {
        row {
            cell(TextFieldWithCompletion(project, createBranchNameCompletion(), branchName,
                /*oneLineMode*/ true,
                /*autoPopup*/ true,
                /*forceAutoPopup*/ false,
                /*showHint*/ false))
                .bind({ c -> c.text }, { c, v -> c.text = v }, ::branchName.toMutableProperty())
                .align(AlignX.FILL)
                .label(MyGitBundle.message("new.branch.dialog.branch.name"), LabelPosition.TOP)
                .focused()
                .applyToComponent {
                    selectAll()
                }
                .validationOnApply(validateBranchName())
        }
        val testBox = JCheckBox(MyGitBundle.message("new.branch.dialog.test.branch.checkbox"))
        val uatBox = JCheckBox(MyGitBundle.message("new.branch.dialog.uat.branch.checkbox"))
        val masterBox = JCheckBox(MyGitBundle.message("new.branch.dialog.master.branch.checkbox"))
        val allBranchNames = getBranchNameList()
        row {
            cell(testBox)
                .bindSelected(::test)
                .applyToComponent {
                    isEnabled = true
                }.component
            cell(uatBox)
                .bindSelected(::uat)
                .applyToComponent {
                    isEnabled = true
                }.component
            cell(masterBox)
                .bindSelected(::master)
                .applyToComponent {
                    isEnabled = true
                }.component
        }
    }

    private fun isBranchExist(branchNames: List<String>, name: String): Boolean {
        return branchNames.contains(name)
    }

    private fun getBranchNameList(): List<String> {
        val localBranches = collectLocalBranchNames()
        val remoteBranches = collectRemoteBranchNames()
        val allBranches = mutableSetOf<String>()
        allBranches += localBranches
        allBranches += remoteBranches
        return allBranches.toList()
    }

    private fun createBranchNameCompletion(): BranchNamesCompletion {
        val localBranches = collectLocalBranchNames()
        val remoteBranches = collectRemoteBranchNames()
        val localDirectories = collectDirectories(localBranches.asIterable(), true)
        val remoteDirectories = collectDirectories(remoteBranches.asIterable(), true)

        val allSuggestions = mutableSetOf<String>()
        allSuggestions += localBranches
        allSuggestions += remoteBranches
        allSuggestions += localDirectories
        allSuggestions += remoteDirectories
        return BranchNamesCompletion(localDirectories.toList(), allSuggestions.toList())
    }

    private fun collectLocalBranchNames() = repositories.asSequence().flatMap { it.branches.localBranches }.map { it.name }
    private fun collectRemoteBranchNames() = repositories.asSequence().flatMap { it.branches.remoteBranches }.map { it.nameForRemoteOperations }

    private fun collectDirectories(branchNames: Iterable<String>, withTrailingSlash: Boolean): Collection<String> {
        val directories = mutableSetOf<String>()
        for (branchName in branchNames) {
            if (branchName.contains(NAME_SEPARATOR)) {
                var index = 0
                while (index < branchName.length) {
                    val end = branchName.indexOf(NAME_SEPARATOR, index)
                    if (end == -1) break
                    directories += if (withTrailingSlash) branchName.substring(0, end + 1) else branchName.substring(0, end)
                    index = end + 1
                }
            }
        }
        return directories
    }

    private fun validateBranchName()
            : ValidationInfoBuilder.(TextFieldWithCompletion) -> ValidationInfo? = {

        // Do not change Document inside DocumentListener callback
        invokeLater {
            it.cleanBranchNameAndAdjustCursorIfNeeded()
        }

        val branchName = validator.cleanUpBranchName(it.text).trim()
        val errorInfo = checkRefNameEmptyOrHead(branchName)
            ?: conflictsWithRemoteBranch(repositories, branchName)
            ?: conflictsWithLocalBranchDirectory(localBranchDirectories, branchName)
        if (errorInfo != null) error(errorInfo.message)
        else {
            val localBranchConflict = conflictsWithLocalBranch(repositories, branchName)
            if (localBranchConflict == null) null // no conflicts or ask to reset
            else error(localBranchConflict.message)
        }
    }

    private fun TextFieldWithCompletion.cleanBranchNameAndAdjustCursorIfNeeded() {
        if (isDisposed) return

        val initialText = text
        val initialCaret = caretModel.offset

        val fixedText = validator.cleanUpBranchNameOnTyping(initialText)

        // if the text didn't change, there's no point in updating it or cursorPosition
        if (fixedText == initialText) return

        val initialTextBeforeCaret = initialText.take(initialCaret)
        val fixedTextBeforeCaret = validator.cleanUpBranchNameOnTyping(initialTextBeforeCaret)

        val fixedCaret = fixedTextBeforeCaret.length

        text = fixedText
        caretModel.moveToOffset(fixedCaret)
    }

    private class BranchNamesCompletion(
        val localDirectories: List<String>,
        val allSuggestions: List<String>)
        : TextCompletionProviderBase<String>(
        DefaultTextCompletionValueDescriptor.StringValueDescriptor(),
        emptyList(),
        false
    ), DumbAware {
        override fun getValues(parameters: CompletionParameters, prefix: String, result: CompletionResultSet): Collection<String> {
            return if (parameters.isAutoPopup) {
                localDirectories
            } else {
                allSuggestions
            }
        }
    }
}
