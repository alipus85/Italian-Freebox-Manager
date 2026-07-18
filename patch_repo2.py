import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_methods = """
    suspend fun removeFile(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.removeFiles(token, RmRequest(listOf(base64Path)))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to remove file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(parentPath: String, dirName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Parent = android.util.Base64.encodeToString(parentPath.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.createDirectory(token, MkdirRequest(base64Parent, dirName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to create directory"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(srcPath: String, newName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Src = android.util.Base64.encodeToString(srcPath.toByteArray(), android.util.Base64.NO_WRAP)
            val response = service.renameFile(token, RenameRequest(base64Src, newName))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDownloadUrl(path: String): String? {
        val token = currentSessionToken ?: return null
        val baseUrl = _boxUrl.value
        val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
        // Adjust for API version
        val actualBaseUrl = _discoveredApiBaseUrl.value
        val actualMajorVersion = _discoveredApiVersionMajor.value
        val rawPrefix = if (actualBaseUrl.endsWith("/")) "${actualBaseUrl}v${actualMajorVersion}/" else "${actualBaseUrl}/v${actualMajorVersion}/"
        val cleanPrefix = rawPrefix.replace("//", "/")
        return "${baseUrl.removeSuffix("/")}${cleanPrefix}dl/$base64Path"
    }
}
"""
    content = content.replace("    }\n}", "    }\n" + new_methods)
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/repository/FreeboxRepository.kt")
