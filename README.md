# LLMDog - IntelliJ Plugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

LLMDog is an IntelliJ Platform plugin that generates Markdown reports for selected files and directories directly within the IDE. It’s designed to streamline documentation and code review workflows for projects leveraging large language models.

## Features

- **Seamless IDE Integration:** Access LLMDog via a dedicated tool window in IntelliJ IDEA.
- **Selective Reporting:** Choose files and directories using an intuitive checkbox tree view.
- **Recursive Inclusion:** Automatically processes all files within selected folders.
- **Gitignore Compliance:** Honors `.gitignore` rules to filter out unwanted files.
- **Markdown Generation:** Produces a clean Markdown report and copies it to your clipboard.

## Installation

> **Note:** LLMDog is not yet available on the JetBrains Marketplace.

For manual installation:

1. Open **Settings/Preferences** in IntelliJ IDEA.
2. Navigate to **Plugins** and choose to install a plugin from disk.
3. Select the downloaded plugin package and restart IntelliJ IDEA.

## Usage

1. Open the **LLMDog Tool Window** via `View` → `Tool Windows` → `LLMDog`.
2. Select the desired files and directories.
3. Click **"Let's Dog It!"** to generate the Markdown report (automatically copied to your clipboard).

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Implement your changes and test thoroughly.
4. Submit a pull request.

## Development

1. Clone the repository:
   ```bash
   git clone https://github.com/doganarif/llmdog-intellij-plugin.git