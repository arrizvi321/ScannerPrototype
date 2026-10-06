package com.example.scannerprototype.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CheckInResponse(
    val ok: Boolean,
    val status: String?,
    val firstName: String?,
    val lastName: String?,
    val visitorName: String?,
    val visitId: String?,
    val message: String?,
    val checkedInAt: String?,
    val checkedOutAt: String?,
    val purpose: String? = null
)

sealed interface CheckInUiState {
    object Idle : CheckInUiState
    object Loading : CheckInUiState
    data class Success(val response: CheckInResponse) : CheckInUiState
    data class Error(val errorMessage: String) : CheckInUiState
}

object CheckInApi {
    private const val PASS_CHECKIN_ENDPOINT_URL = "http://192.168.50.178:3000/api/check-in-with-pass"
    private const val SIGN_IN_ENDPOINT_URL = "http://192.168.50.178:3000/api/sign-in"
    private const val SIGN_OUT_ENDPOINT_URL = "http://192.168.50.178:3000/api/sign-out"

    suspend fun checkInWithPass(tokenValue: String): Result<CheckInResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(PASS_CHECKIN_ENDPOINT_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                put("token_value", tokenValue)
            }

            connection.outputStream.use { os ->
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes, 0, bytes.size)
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val json = JSONObject(responseBody)
                val response = CheckInResponse(
                    ok = json.optBoolean("ok", false),
                    status = extractProperty(json, "status"),
                    firstName = extractProperty(json, "firstName", "first_name"),
                    lastName = extractProperty(json, "lastName", "last_name"),
                    visitorName = extractProperty(json, "visitorName", "visitor_name", "name"),
                    visitId = extractProperty(json, "visitId", "visit_id", "id"),
                    message = extractProperty(json, "message"),
                    checkedInAt = extractProperty(json, "checkedInAt", "checked_in_at"),
                    checkedOutAt = extractProperty(json, "checkedOutAt", "checked_out_at"),
                    purpose = extractProperty(json, "purpose", "visit_purpose", "purpose_of_visit", "purposeOfVisit")
                )
                Result.success(response)
            } else {
                val errorMessage = try {
                    JSONObject(responseBody).optNullableString("message") ?: "HTTP Error $responseCode"
                } catch (_: Exception) {
                    "HTTP Error $responseCode"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInManual(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        purpose: String,
        symptoms: String
    ): Result<CheckInResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(SIGN_IN_ENDPOINT_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                put("firstName", firstName)
                put("lastName", lastName)
                put("phoneNumber", phoneNumber)
                put("purpose", purpose)
                put("symptoms", symptoms)
            }

            connection.outputStream.use { os ->
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes, 0, bytes.size)
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val json = JSONObject(responseBody)
                val response = CheckInResponse(
                    ok = json.optBoolean("ok", false),
                    status = extractProperty(json, "status"),
                    firstName = extractProperty(json, "firstName", "first_name") ?: firstName,
                    lastName = extractProperty(json, "lastName", "last_name") ?: lastName,
                    visitorName = extractProperty(json, "visitorName", "visitor_name", "name") ?: "$firstName $lastName",
                    visitId = extractProperty(json, "visitId", "visit_id", "id"),
                    message = extractProperty(json, "message"),
                    checkedInAt = extractProperty(json, "checkedInAt", "checked_in_at"),
                    checkedOutAt = extractProperty(json, "checkedOutAt", "checked_out_at"),
                    purpose = extractProperty(json, "purpose", "visit_purpose", "purpose_of_visit", "purposeOfVisit") ?: purpose
                )
                Result.success(response)
            } else {
                val errorMessage = try {
                    JSONObject(responseBody).optNullableString("message") ?: "HTTP Error $responseCode"
                } catch (_: Exception) {
                    "HTTP Error $responseCode"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOutManual(
        phoneNumber: String
    ): Result<CheckInResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(SIGN_OUT_ENDPOINT_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            val payload = JSONObject().apply {
                put("phoneNumber", phoneNumber)
            }

            connection.outputStream.use { os ->
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes, 0, bytes.size)
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode in 200..299) {
                val json = JSONObject(responseBody)
                val response = CheckInResponse(
                    ok = json.optBoolean("ok", false),
                    status = extractProperty(json, "status"),
                    firstName = extractProperty(json, "firstName", "first_name"),
                    lastName = extractProperty(json, "lastName", "last_name"),
                    visitorName = extractProperty(json, "visitorName", "visitor_name", "name"),
                    visitId = extractProperty(json, "visitId", "visit_id", "id"),
                    message = extractProperty(json, "message"),
                    checkedInAt = extractProperty(json, "checkedInAt", "checked_in_at"),
                    checkedOutAt = extractProperty(json, "checkedOutAt", "checked_out_at"),
                    purpose = extractProperty(json, "purpose", "visit_purpose", "purpose_of_visit", "purposeOfVisit")
                )
                Result.success(response)
            } else {
                val errorMessage = try {
                    JSONObject(responseBody).optNullableString("message") ?: "HTTP Error $responseCode"
                } catch (_: Exception) {
                    "HTTP Error $responseCode"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key) else null
    }

    private fun extractProperty(json: JSONObject, vararg keys: String): String? {
        // 1. Search top-level keys
        for (k in keys) {
            val value = json.optNullableString(k)
            if (!value.isNullOrBlank()) return value
        }

        // 2. Search common nested sub-objects (e.g. visitor, pass, visit, data, details)
        val subObjectNames = listOf("visitor", "pass", "visit", "data", "details")
        for (subName in subObjectNames) {
            val subObj = json.optJSONObject(subName) ?: continue
            for (k in keys) {
                val value = subObj.optNullableString(k)
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }
}
