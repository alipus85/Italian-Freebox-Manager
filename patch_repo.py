import sys

def modify_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add imports
    imports_to_add = """import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.crypto.Mac"""
    content = content.replace("import java.util.concurrent.TimeUnit\nimport javax.crypto.Mac", imports_to_add)

    # Add getUnsafeOkHttpClientBuilder before rebuildApiService
    unsafe_builder = """    }

    private fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun rebuildApiService() {"""
    content = content.replace("    }\n\n    private fun rebuildApiService() {", unsafe_builder)

    # Replace OkHttpClient.Builder() with getUnsafeOkHttpClientBuilder()
    content = content.replace("val client = OkHttpClient.Builder()", "val client = getUnsafeOkHttpClientBuilder()")
    content = content.replace("val tempClient = OkHttpClient.Builder()", "val tempClient = getUnsafeOkHttpClientBuilder()")

    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    modify_file("app/src/main/java/com/example/data/repository/FreeboxRepository.kt")
