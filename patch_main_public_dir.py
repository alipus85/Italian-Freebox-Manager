import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_dir = """
                            val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val destFile = java.io.File(destDir, name)
                            viewModel.downloadFileToDisk(fullPath, destFile)
                            Toast.makeText(context, "Download avviato in Downloads", Toast.LENGTH_SHORT).show()
"""
    import re
    content = re.sub(r'val destDir = context\.getExternalFilesDir\(Environment\.DIRECTORY_DOWNLOADS\)\n\s*val destFile = java\.io\.File\(destDir, name\)\n\s*viewModel\.downloadFileToDisk\(fullPath, destFile\)\n\s*Toast\.makeText\(context, "Download avviato in app data/Downloads", Toast\.LENGTH_SHORT\)\.show\(\)', new_dir.strip(), content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/MainActivity.kt")
