import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_methods = """
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

    suspend fun downloadFileToDisk(path: String, destFile: java.io.File): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val url = getDownloadUrl(path) ?: return@withContext Result.failure(Exception("Could not generate URL"))
            val response = service.downloadFile(url, token)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.byteStream().use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to download file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
"""
    content = content.replace("    fun getDownloadUrl(path: String): String? {\n        val token = currentSessionToken ?: return null\n        val baseUrl = _boxUrl.value\n        val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)\n        // Adjust for API version\n        val actualBaseUrl = _discoveredApiBaseUrl.value\n        val actualMajorVersion = _discoveredApiVersionMajor.value\n        val rawPrefix = if (actualBaseUrl.endsWith(\"/\")) \"${actualBaseUrl}v${actualMajorVersion}/\" else \"${actualBaseUrl}/v${actualMajorVersion}/\"\n        val cleanPrefix = rawPrefix.replace(\"//\", \"/\")\n        return \"${baseUrl.removeSuffix(\"/\")}${cleanPrefix}dl/$base64Path\"\n    }\n}", new_methods)
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/repository/FreeboxRepository.kt")
