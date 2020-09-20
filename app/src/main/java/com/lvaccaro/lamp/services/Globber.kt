package com.lvaccaro.lamp.services

import java.io.*
import java.util.logging.Logger

class Globber(val stream: InputStream, val file: File): Thread() {
    val log = Logger.getLogger(LightningService::class.java.name)

    override fun run() {
        try {
            val isr = InputStreamReader(stream)
            val br = BufferedReader(isr)
            while(!Thread.currentThread().isInterrupted()) {
                val line = br.readLine()
                if (line != null && line.length > 0) {
                    file.appendText("${line}\n")
                    log.info(line)
                }
            }
        } catch (ioe: Exception) {
            ioe.printStackTrace()
        }
    }
}