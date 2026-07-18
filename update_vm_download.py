import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_methods = """
    fun downloadFileToDisk(path: String, destFile: java.io.File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val result = repository.downloadFileToDisk(path, destFile)
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    error = result.exceptionOrNull()?.message,
                    feedback = if (result.isSuccess) "Download completato" else "Errore download"
                )
            }
        }
    }
    
    fun getSessionToken(): String {
        return repository.sessionToken.value
    }
"""
    content = content.replace("    fun getSessionToken(): String {\n        return repository.sessionToken.value\n    }", new_methods)
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/ui/FreeboxViewModel.kt")
