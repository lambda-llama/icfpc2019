package io.github.lambdallama

import com.beust.klaxon.*
import java.io.StringReader

sealed class JsValidatorResult {
    class Success(val time: Int): JsValidatorResult()
    class Failure(val error: String): JsValidatorResult()
}

fun validateWithJsValidator(desc: String, sol: String): JsValidatorResult {
    val result = ProcessBuilder("node", "validator/main.js", desc, sol)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start().apply {
                waitFor()
            }.inputStream.bufferedReader().readText()
    val obj = Parser.default().parse(StringReader(result)) as JsonObject
    if (obj.boolean("success")!!) {
        return JsValidatorResult.Success(obj.int("time")!!)
    } else {
        return JsValidatorResult.Failure(obj.string("error")!!)
    }
}
