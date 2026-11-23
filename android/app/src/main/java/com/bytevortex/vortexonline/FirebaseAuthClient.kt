package com.bytevortex.vortexonline

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Firebase email/password auth via the documented Identity Toolkit REST API. */
class FirebaseAuthClient(private val apiKey: String) {
    data class User(val email: String, val idToken: String, val refreshToken: String)

    fun signIn(email: String, password: String): User = authRequest("accounts:signInWithPassword", email, password)

    fun signUp(email: String, password: String): User = authRequest("accounts:signUp", email, password)

    fun refreshSession(refreshToken: String, fallbackEmail: String): User {
        if (apiKey.isBlank()) throw IllegalStateException("A chave Firebase não está configurada nesta build.")
        val url = URL("https://securetoken.googleapis.com/v1/token?key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            val body = "grant_type=refresh_token&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) throw IllegalStateException(humanError(json.optJSONObject("error")?.optString("message").orEmpty()))
            val idToken = json.optString("id_token")
            val nextRefresh = json.optString("refresh_token", refreshToken)
            if (idToken.isBlank()) throw IllegalStateException("O Firebase não retornou uma sessão válida.")
            return User(json.optString("user_email", fallbackEmail), idToken, nextRefresh)
        } finally {
            connection.disconnect()
        }
    }

    fun sendPasswordReset(email: String) {
        val response = post("accounts:sendOobCode", JSONObject()
            .put("requestType", "PASSWORD_RESET")
            .put("email", email))
        if (!response.optString("email").equals(email, ignoreCase = true)) {
            throw IllegalStateException("O Firebase não confirmou o envio da recuperação.")
        }
    }

    private fun authRequest(method: String, email: String, password: String): User {
        val response = post(method, JSONObject()
            .put("email", email.trim())
            .put("password", password)
            .put("returnSecureToken", true))
        val resultEmail = response.optString("email", email.trim())
        val token = response.optString("idToken")
        val refreshToken = response.optString("refreshToken")
        if (token.isBlank() || refreshToken.isBlank()) throw IllegalStateException("O Firebase não retornou uma sessão válida.")
        return User(resultEmail, token, refreshToken)
    }

    private fun post(method: String, body: JSONObject): JSONObject {
        if (apiKey.isBlank()) throw IllegalStateException("A chave Firebase não está configurada nesta build.")
        val url = URL("https://identitytoolkit.googleapis.com/v1/$method?key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) {
                val raw = json.optJSONObject("error")?.optString("message").orEmpty()
                throw IllegalStateException(humanError(raw))
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun humanError(code: String): String = when {
        code.contains("OPERATION_NOT_ALLOWED") -> "Ative E-mail/senha nos métodos de login do Firebase Authentication."
        code.contains("EMAIL_EXISTS") -> "Esse e-mail já tem conta. Tente entrar."
        code.contains("INVALID_LOGIN_CREDENTIALS") || code.contains("EMAIL_NOT_FOUND") || code.contains("INVALID_PASSWORD") -> "E-mail ou senha incorretos."
        code.contains("WEAK_PASSWORD") -> "A senha precisa ter pelo menos 6 caracteres."
        code.contains("INVALID_EMAIL") -> "Esse endereço de e-mail não parece válido."
        code.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Muitas tentativas. Aguarde um pouco e tente novamente."
        code.contains("USER_DISABLED") -> "Esta conta foi desativada no Firebase."
        code.contains("API_KEY_INVALID") -> "A chave Firebase configurada para este app não foi aceita."
        code.contains("API_KEY_SERVICE_BLOCKED") -> "A chave Firebase está bloqueada para Authentication; confira as restrições da chave."
        else -> "Falha na autenticação Firebase${if (code.isNotBlank()) ": $code" else ". Confira sua internet e a configuração do projeto."}"
    }
}
