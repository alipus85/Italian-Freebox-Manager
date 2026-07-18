import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_download_logic = """
                            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            val destFile = java.io.File(destDir, name)
                            viewModel.downloadFileToDisk(fullPath, destFile)
                            Toast.makeText(context, "Download avviato in app data/Downloads", Toast.LENGTH_SHORT).show()
"""
    
    import re
    # We replace the DownloadManager logic
    content = re.sub(r'val url = viewModel\.getDownloadUrl\(fullPath\)\s*if \(url != null\) \{.*Toast\.makeText\(context, "Download avviato", Toast\.LENGTH_SHORT\)\.show\(\)\s*\}', new_download_logic.strip(), content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/MainActivity.kt")
