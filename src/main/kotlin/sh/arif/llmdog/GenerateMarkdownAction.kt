package sh.arif.llmdog

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.io.IOException
import com.intellij.openapi.wm.ToolWindowManager

class GenerateMarkdownAction : AnAction("Generate Markdown for LLM (LLMDog)") {

    private val logger = Logger.getInstance(GenerateMarkdownAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(PlatformDataKeys.PROJECT) ?: run {
            logger.warn("No project found in the event.")
            Messages.showErrorDialog("No project selected. Please open a project first.", "LLMDog Error")
            return
        }

        // Get the ToolWindow manager
        val toolWindowManager = ToolWindowManager.getInstance(project)

        // Get the LLMDog tool window
        val toolWindow = toolWindowManager.getToolWindow("LLMDog")

        // If the LLMDog tool window content is an instance of the LLMDogSelectionDialog:
        val selectionDialog = toolWindow?.contentManager?.contents?.getOrNull(0)?.component as? LLMDogSelectionDialog

        if (selectionDialog == null) {
            logger.error("Could not find the LLMDogSelectionDialog in the tool window.")
            Messages.showErrorDialog("Error: Could not access LLMDog file selection tool.", "LLMDog Error")
            return
        }

        val selectedFiles = selectionDialog.getSelectedFiles()
        if (selectedFiles.isEmpty()) {
            Messages.showWarningDialog("No files selected in the dialog.", "LLMDog Warning")
            return
        }

        try {
            val markdown = generateMarkdown(project, selectedFiles)
            copyToClipboard(markdown)
            Messages.showMessageDialog(project, "Markdown copied to clipboard!", "LLMDog - arif.sh", Messages.getInformationIcon())
        } catch (ex: IOException) {
            logger.error("IO Exception during markdown generation or clipboard copy: ", ex)
            Messages.showErrorDialog(project, "Error: ${ex.message}", "LLMDog Error")
        } catch (ex: Exception) {
            logger.error("Unexpected error: ", ex)
            Messages.showErrorDialog(project, "An unexpected error occurred: ${ex.message}", "LLMDog Error")
        }
    }

    private fun generateMarkdown(project: Project, files: List<VirtualFile>): String {
        val sb = StringBuilder()

        sb.append("# Directory Structure\n```\n")
        for (file in files) {
            sb.append(buildFileTree(project, file, ""))
        }
        sb.append("```\n\n")

        sb.append("# File Contents\n")
        for (file in files) {
            if (!file.isDirectory) {
                try {
                    val content = String(file.contentsToByteArray()) // Read content
                    val fileType = file.fileType.name.lowercase() // Get file extension (rough estimate)
                    sb.append("## File: ${file.path}\n")
                    sb.append("```$fileType\n")
                    sb.append(content)
                    sb.append("\n```\n\n")
                } catch (e: IOException) {
                    logger.warn("Could not read file ${file.path}: ${e.message}")
                    sb.append("## File: ${file.path}\n")
                    sb.append("_(Error reading file content: ${e.message})_\n\n")
                }
            }
        }
        return sb.toString()
    }

    private fun buildFileTree(project: Project, file: VirtualFile, indent: String): String {
        val sb = StringBuilder()

        if (file.isDirectory) {
            sb.append("$indent${file.name}/\n")
            for (child in file.children) {
                sb.append(buildFileTree(project, child, "$indent  "))
            }
        } else {
            sb.append("$indent${file.name}\n")
        }
        return sb.toString()
    }

    private fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}