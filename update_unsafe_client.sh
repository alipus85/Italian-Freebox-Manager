#!/bin/bash
cat << 'INNER_EOF' > /tmp/unsafe_client.patch
--- app/src/main/java/com/example/data/repository/FreeboxRepository.kt
+++ app/src/main/java/com/example/data/repository/FreeboxRepository.kt
@@ -19,6 +19,10 @@
 import retrofit2.Retrofit
 import retrofit2.converter.moshi.MoshiConverterFactory
 import java.util.concurrent.TimeUnit
+import java.security.SecureRandom
+import java.security.cert.X509Certificate
+import javax.net.ssl.SSLContext
+import javax.net.ssl.TrustManager
+import javax.net.ssl.X509TrustManager
 import javax.crypto.Mac
 import javax.crypto.spec.SecretKeySpec
 
@@ -165,6 +169,21 @@
         }
     }
 
+    private fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
+        return try {
+            val trustAllCerts = arrayOf<TrustManager>(
+                object : X509TrustManager {
+                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
+                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
+                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
+                }
+            )
+            val sslContext = SSLContext.getInstance("SSL")
+            sslContext.init(null, trustAllCerts, SecureRandom())
+            val sslSocketFactory = sslContext.socketFactory
+
+            OkHttpClient.Builder()
+                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
+                .hostnameVerifier { _, _ -> true }
+        } catch (e: Exception) {
+            throw RuntimeException(e)
+        }
+    }
+
     private fun rebuildApiService() {
         try {
             val moshi = Moshi.Builder()
@@ -207,7 +226,7 @@
                 }
             }
 
-            val client = OkHttpClient.Builder()
+            val client = getUnsafeOkHttpClientBuilder()
                 .connectTimeout(5, TimeUnit.SECONDS)
                 .readTimeout(5, TimeUnit.SECONDS)
                 .writeTimeout(5, TimeUnit.SECONDS)
@@ -226,7 +245,7 @@
             return@withContext Result.success("http://myiliadbox.iliad.it/")
         }
         try {
-            val tempClient = OkHttpClient.Builder()
+            val tempClient = getUnsafeOkHttpClientBuilder()
                 .connectTimeout(3, TimeUnit.SECONDS)
                 .readTimeout(3, TimeUnit.SECONDS)
                 .build()
INNER_EOF
patch -p0 < /tmp/unsafe_client.patch
