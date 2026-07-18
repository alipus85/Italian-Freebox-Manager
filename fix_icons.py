import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Imports
    content = content.replace("import androidx.compose.material.icons.filled.Download", "")
    content = content.replace("import androidx.compose.material.icons.filled.CreateNewFolder", "import androidx.compose.material.icons.filled.Add")
    content = content.replace("import androidx.compose.material.icons.filled.Upload", "import androidx.compose.material.icons.filled.KeyboardArrowUp")
    content = content.replace("import android.net.Uri", "import android.net.Uri\nimport android.widget.Toast")

    # Usages
    content = content.replace("Icons.Default.Upload", "Icons.Default.KeyboardArrowUp")
    content = content.replace("Icons.Default.CreateNewFolder", "Icons.Default.Add")
    content = content.replace("Icons.Default.Download", "Icons.Default.KeyboardArrowDown") # Or similar
    content = content.replace("Icons.Default.KeyboardArrowDown", "androidx.compose.material.icons.filled.KeyboardArrowDown")

    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/MainActivity.kt")
