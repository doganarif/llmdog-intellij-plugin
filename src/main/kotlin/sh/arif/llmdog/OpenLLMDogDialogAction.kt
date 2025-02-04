package sh.arif.llmdog

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.Project

class OpenLLMDogDialogAction : AnAction("LLMDog: Select Files") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.getData(PlatformDataKeys.PROJECT)
        if (project != null) {
            // Get the ToolWindow manager
            val toolWindowManager = ToolWindowManager.getInstance(project)

            // Get the LLMDog tool window by ID
            val toolWindow = toolWindowManager.getToolWindow("LLMDog")

            // Activate the tool window (if it's not already visible)
            toolWindow?.show()
        }
    }
}