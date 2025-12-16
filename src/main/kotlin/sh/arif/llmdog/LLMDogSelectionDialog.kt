package sh.arif.llmdog

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.*
import java.io.File
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.io.IOException

class LLMDogSelectionDialog(private val project: Project) : JPanel(BorderLayout()) {

    private lateinit var fileTree: JTree
    private lateinit var rootNode: CheckableTreeNode
    private val selectedFiles = mutableSetOf<VirtualFile>()
    private val generateButton = JButton("Let's Dog It!") // Changed button text
    private val logger = Logger.getInstance(LLMDogSelectionDialog::class.java)

    // Store a set of selected folders to efficiently check parent selection state
    private val selectedFolders = mutableSetOf<VirtualFile>()

    init {
        preferredSize = Dimension(600, 400) // Set a reasonable size

        rootNode = project.basePath?.let { CheckableTreeNode(it) }!! // Use CheckableTreeNode
        val treeModel = DefaultTreeModel(rootNode)
        fileTree = JTree(treeModel)
        fileTree.isRootVisible = false
        fileTree.showsRootHandles = true

        // Set the custom cell renderer
        fileTree.cellRenderer = CheckBoxTreeCellRenderer(this) // Pass the dialog instance

        // Enable multiple selections (though checkboxes handle the real selection)
        fileTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION

        // Add a mouse listener to handle checkbox clicks
        fileTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val selRow = fileTree.getRowForLocation(e.x, e.y)
                val path = fileTree.getPathForLocation(e.x, e.y)
                if (selRow != -1 && path != null) {
                    val node = path.lastPathComponent as? CheckableTreeNode
                    if (node != null) {
                        val userObject = node.userObject
                        val pathStr = (userObject as? VirtualFile)?.path ?: userObject.toString()
                        logger.info("Clicked node: $pathStr currentChecked=${node.isChecked} -> toggling")
                        node.isChecked = !node.isChecked // Toggle the state
                        (treeModel as DefaultTreeModel).nodeChanged(node)
                        updateSelectedFiles(node) // Update
                    }
                }
            }
        })

        add(JScrollPane(fileTree), BorderLayout.CENTER)

        // Create a panel for the button
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        buttonPanel.add(generateButton)
        add(buttonPanel, BorderLayout.SOUTH) // Add the button to the bottom

        project.basePath?.let {
            val basePathFile = File(it)
            if (basePathFile.exists()) {
                val rootVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(basePathFile)
                if (rootVirtualFile != null) {
                    fillTree(rootVirtualFile, rootNode)
                    (treeModel as DefaultTreeModel).reload() // Notify the tree to redraw
                    collapseAllNodes(fileTree, 0, fileTree.rowCount)
                } else {
                    println("Could not find VirtualFile for project base path.")
                }
            } else {
                println("Project base path does not exist: $it")
            }
        }

        // Add an ActionListener to the button
        generateButton.addActionListener {
            generateMarkdown() // Calls the new function
        }
    }

    private fun fillTree(virtualFile: VirtualFile, parentNode: CheckableTreeNode) {
        val files = VfsUtil.getChildren(virtualFile)

        if (files != null) {
            for (childVirtualFile in files) {
                val childNode = CheckableTreeNode(childVirtualFile) // Use CheckableTreeNode
                parentNode.add(childNode)
                fillNode(childNode, childVirtualFile)
            }
        }
    }

    private fun fillNode(parentNode: CheckableTreeNode, virtualFile: VirtualFile) {
        if (virtualFile.isDirectory) {
            val files = VfsUtil.getChildren(virtualFile)
            if (files != null) {
                for (childVirtualFile in files) {
                    val childNode = CheckableTreeNode(childVirtualFile)  // Use CheckableTreeNode
                    parentNode.add(childNode)
                    fillNode(childNode, childVirtualFile)
                }
            }
        }
    }

    fun getSelectedFiles(): Set<VirtualFile> {
        return selectedFiles.toSet()
    }

    // update selected file
    private fun updateSelectedFiles(node : CheckableTreeNode) {
        val userObject = node.userObject
        if (userObject is VirtualFile) {
            if (node.isChecked) {
                selectedFiles.add(userObject)
                selectedFolders.add(userObject)
                //After set parent now update child
                setChildrenSelected(node, true)
            } else {
                selectedFiles.remove(userObject)
                selectedFolders.remove(userObject)

                setChildrenSelected(node, false)
            }
        }
    }

    //Set all childs selected
    private fun setChildrenSelected(node: CheckableTreeNode, selected: Boolean) {
        if (!node.isLeaf) {
            val children = node.children()
            while (children.hasMoreElements()) {
                val child = children.nextElement() as CheckableTreeNode
                child.isChecked = selected // Set the checkbox state
                (fileTree.model as DefaultTreeModel).nodeChanged(child)
                val userObject = child.userObject
                if (userObject is VirtualFile) {
                    if (selected) {
                        selectedFiles.add(userObject)
                        if(userObject.isDirectory)
                        {
                            selectedFolders.add(userObject)
                        }

                    } else {
                        selectedFiles.remove(userObject)
                        selectedFolders.remove(userObject)
                    }
                }

                setChildrenSelected(child, selected) // Recursive call
            }
        }
    }

    //Function to collapse all nodes in the tree
    private fun collapseAllNodes(tree: JTree, startingIndex: Int, rowCount: Int) {
        for (i in startingIndex until rowCount) {
            tree.collapseRow(i)
        }

        if (tree.rowCount != rowCount) {
            collapseAllNodes(tree, rowCount, tree.rowCount)
        }
    }

    //Function to perform the markdown
    private fun generateMarkdown() {
        val selectedFiles = getSelectedFiles() // Get selected files

        if (selectedFiles.isEmpty()) {
            Messages.showWarningDialog("No files selected!  Please select files or folders.", "LLMDog Warning") // Improved message
            return
        }

        try {
            val markdown = buildMarkdownOutput(project, selectedFiles) // Generate Markdown
            copyToClipboard(markdown) // Copy to clipboard
            Messages.showMessageDialog(project, "Markdown report generated and copied to clipboard!  Woof!", "LLMDog", Messages.getInformationIcon()) // Improved message
        } catch (ex: IOException) {
            logger.error("IO Exception during markdown generation or clipboard copy: ", ex)
            Messages.showErrorDialog(project, "Error: ${ex.message}", "LLMDog Error")
        } catch (ex: Exception) {
            logger.error("Unexpected error: ", ex)
            Messages.showErrorDialog(project, "An unexpected error occurred: ${ex.message}", "LLMDog Error")
        }
    }

    private fun buildMarkdownOutput(project: Project, files: Set<VirtualFile>): String {
        val sb = StringBuilder()

        sb.append("# Directory Structure\n```\n")
        val nodesAddedToFileTree = mutableSetOf<VirtualFile>()

        for (file in files) {
            sb.append(buildFileTree(project, file, files, nodesAddedToFileTree, ""))
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


    private fun buildFileTree(project: Project, file: VirtualFile, filesToInclude: Set<VirtualFile>, nodesAlreadyAdded: MutableSet<VirtualFile>, indent: String): String {
        if (file !in filesToInclude) return ""
        if (file in nodesAlreadyAdded) return ""
        nodesAlreadyAdded.add(file)

        val sb = StringBuilder()

        if (file.isDirectory) {
            sb.append("$indent${file.name}/\n")
            for (child in file.children) {
                sb.append(buildFileTree(project, child, filesToInclude, nodesAlreadyAdded, "$indent  "))
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

    private fun collectAllFiles(dir: VirtualFile, files: MutableList<VirtualFile>) {
        for (child in VfsUtil.getChildren(dir)) {
            if (child.isDirectory) {
                collectAllFiles(child, files) // Recursive call
            } else {
                files.add(child) // Add the file
            }
        }
    }

    fun isFolderSelected(file:VirtualFile):Boolean{
        return selectedFolders.contains(file)
    }
}

// Custom TreeNode that holds a boolean to represent checked state
class CheckableTreeNode(userObject: Any) : DefaultMutableTreeNode(userObject) {
    var isChecked: Boolean = false
}

// Custom TreeCellRenderer to render a checkbox in each tree node
class CheckBoxTreeCellRenderer(val dialog: LLMDogSelectionDialog) : TreeCellRenderer { // Receive dialog instance

    private val checkBox = JCheckBox()
    private val label = JLabel()
    private val panel = JPanel(BorderLayout())

    // Load the icons
    private val folderIcon = IconLoader.getIcon("/icons/folder.svg", CheckBoxTreeCellRenderer::class.java) // Replace with your folder icon
    private val fileIcon = IconLoader.getIcon("/icons/file.svg", CheckBoxTreeCellRenderer::class.java) // Replace with your file icon

    init {
        panel.add(checkBox, BorderLayout.WEST)
        panel.add(label, BorderLayout.CENTER)
        panel.isOpaque = false
    }

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        if (value is CheckableTreeNode) {
            val userObject = value.userObject
            if (userObject is VirtualFile) {
                label.text = userObject.name

                // Set the icon based on file type
                if (userObject.isDirectory) {
                    label.icon = folderIcon
                    label.font = label.font.deriveFont(Font.BOLD) // Make directory names bold
                } else {
                    label.icon = fileIcon
                    label.font = label.font.deriveFont(Font.PLAIN) // Use regular font for files
                }

                // Check parent dir
                if (dialog.isFolderSelected(userObject)) {
                    checkBox.isSelected = true
                    checkBox.isEnabled = false
                } else {
                    checkBox.isSelected = value.isChecked // Set the checkbox state from the TreeNode
                    checkBox.isEnabled = true
                }

            } else {
                label.text = userObject.toString()
                label.icon = null
                checkBox.isEnabled = true
            }

            label.isOpaque = false
            checkBox.isOpaque = false
            panel.background = if (selected) tree.background else null
            label.foreground = if (selected) tree.foreground else null
        } else {
            label.text = value.toString()
            checkBox.isVisible = false
            label.icon = null
            checkBox.isEnabled = true
        }

        return panel
    }
}