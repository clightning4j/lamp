package com.lvaccaro.lamp.Services

import java.io.*
import java.util.logging.Logger

class Globber(val stream: InputStream, val file: File): Thread() {
    val log = Logger.getLogger(LightningService::class.java.name)

    override fun run() {
        try {
            val isr = InputStreamReader(stream)
            val br = BufferedReader(isr)
            while(!Thread.currentThread().isInterrupted()) {
                val line = br.readLine() + "\n"
                file.appendText(line ?: "")
                log.info(line)
            }
        } catch (ioe: Exception) {
            ioe.printStackTrace()
        }
    }
}