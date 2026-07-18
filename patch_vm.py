import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_methods = """
    fun removeFile(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.removeFile(path)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File rimosso" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun createDirectory(dirName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.createDirectory(_uiState.value.currentPath, dirName)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Cartella creata" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun renameFile(srcPath: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.renameFile(srcPath, newName)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "File rinominato" else null
                )
            }
            if (result.isSuccess) loadFiles(_uiState.value.currentPath)
        }
    }

    fun getDownloadUrl(path: String): String? {
        return repository.getDownloadUrl(path)
    }
    
    fun getSessionToken(): String {
        return repository.sessionToken.value
    }
"""
    content = content.replace("    fun registerApp() {", new_methods + "\n    fun registerApp() {")
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/ui/FreeboxViewModel.kt")
