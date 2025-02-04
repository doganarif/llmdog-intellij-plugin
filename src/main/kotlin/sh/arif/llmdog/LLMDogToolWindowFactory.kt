package sh.arif.llmdog

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel

class LLMDogToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val dialog = LLMDogSelectionDialog(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(dialog, "", false)
        toolWindow.contentManager.addContent(content)
    }
}