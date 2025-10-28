# Farm Manager 2.0 - Dev Container Setup

This project is configured with a development container for a consistent development environment.

## Prerequisites

- Visual Studio Code
- Docker Desktop
- Dev Containers extension for VS Code

## Getting Started

1. **Open in Dev Container**:
   - Open VS Code
   - Open this folder
   - When prompted, click "Reopen in Container" 
   - Or use Command Palette: `Dev Containers: Reopen in Container`

2. **What's Included**:
   - Java 21 LTS
   - Java Extension Pack for VS Code
   - Git and GitHub CLI
   - Pre-configured build and debug tasks

## Development Tasks

### Building the Project
```bash
# Compile all Java files
javac *.java
```

### Running the Application
```bash
# Run the main application
java Main
```

### Using VS Code Tasks
- **Ctrl+Shift+P** → "Tasks: Run Task"
- Available tasks:
  - **Compile Java**: Builds all .java files
  - **Run Farm Manager**: Compiles and runs the application
  - **Clean**: Removes all .class files

### Debugging
- Use **F5** to start debugging
- Set breakpoints in your Java code
- Two debug configurations available:
  - **Run Farm Manager**: Normal execution
  - **Debug Farm Manager**: Debug with remote debugging enabled

## Project Structure

```
FarmManager2.0/
├── .devcontainer/          # Dev container configuration
├── .vscode/               # VS Code workspace settings
├── *.java                # Java source files
├── *.csv                 # Data files
└── *.png                # Resources
```

## CSV Data Files

The application uses these CSV files for data storage:
- `customers.csv` - Customer information
- `inventory.csv` - Inventory items
- `animals.csv` - Animal sales data
- `services.csv` - Service appointments  
- `invoices.csv` - Sales invoices

## Troubleshooting

1. **Container won't start**: Ensure Docker Desktop is running
2. **Java not found**: The container should auto-configure Java 21
3. **Extensions not loading**: Rebuild container with **Ctrl+Shift+P** → "Dev Containers: Rebuild Container"

## Development Tips

- The dev container mounts your local workspace, so file changes persist
- Extensions and settings are automatically configured
- The container includes debugging support on port 5005
- Use the integrated terminal for command-line operations