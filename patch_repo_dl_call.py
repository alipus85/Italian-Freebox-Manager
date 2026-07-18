import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_download = """
    suspend fun downloadFileToDisk(path: String, destFile: java.io.File): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = currentSessionToken ?: return@withContext Result.failure(Exception("No active session"))
        val service = apiService ?: return@withContext Result.failure(Exception("Service not initialized"))
        try {
            val base64Path = android.util.Base64.encodeToString(path.toByteArray(), android.util.Base64.NO_WRAP)
            Log.d("FreeboxDownload", "Downloading from path: $path, base64: $base64Path")
            val response = service.downloadFile(token, base64Path)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.byteStream().use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(true)
            } else {
                Log.e("FreeboxDownload", "Download failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to download file: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("FreeboxDownload", "Download exception", e)
            Result.failure(e)
        }
    }
"""
    import re
    content = re.sub(r'suspend fun downloadFileToDisk.*?Result\.failure\(e\)\n        \}\n    \}', new_download.strip(), content, flags=re.DOTALL)
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/repository/FreeboxRepository.kt")
