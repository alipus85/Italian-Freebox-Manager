import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    new_dl = """
    @GET("api/v8/dl/{path}")
    @Streaming
    suspend fun downloadFile(
        @Header("X-Fbx-App-Auth") sessionToken: String,
        @Path("path", encoded = false) path: String
    ): Response<okhttp3.ResponseBody>
"""
    import re
    content = re.sub(r'@GET\n\s*@Streaming\n\s*suspend fun downloadFile.*?Response<okhttp3.ResponseBody>', new_dl.strip(), content, flags=re.DOTALL)
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/api/FreeboxApi.kt")
