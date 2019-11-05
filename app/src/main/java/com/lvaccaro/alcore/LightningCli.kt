package com.lvaccaro.alcore

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Logger


class LightningCli {

    val command = "lightning-cli"
    val log = Logger.getLogger(LightningService::class.java.name)

    @Throws(Exception::class)
    fun exec(c: Context, options: Array<String>, json: Boolean = true): InputStream {
        val binaryDir = rootDir(c)
        val lightningDir = File(rootDir(c), ".lightning")

        val args =
            arrayOf( String.format("%s/%s", binaryDir.canonicalPath, command),
            String.format("--lightning-dir=%s", lightningDir.path),
            String.format("--%s", if (json === true) "json" else "raw" ))

        val pb = ProcessBuilder((args + options).asList())
        pb.directory(binaryDir)
        //pb.redirectErrorStream(true)

        val process = pb.start()
        val code = process.waitFor()
        if (code == null || code != 0) {
            val error = process.errorStream.toText()
            val input = process.inputStream.toText()
            log.info(error)
            log.info(input)
            throw Exception(if(!error.isEmpty()) error else input)
        }
        return process.inputStream
    }

    fun rootDir(c: Context): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return c.noBackupFilesDir
        }
        return c.filesDir
    }
}

fun InputStream.toText(): String {
    val reader =  bufferedReader()
    val builder = StringBuilder()
    var line = reader.readLine()
    while (line != null) {
        if (!line.startsWith("**")) {
            builder.append(line)
        }
        line = reader.readLine()
    }
    return builder.toString()
}

fun InputStream.toJSONObject(): JSONObject {
    val text = toText()
    val json = JSONObject(text)
    return json
}