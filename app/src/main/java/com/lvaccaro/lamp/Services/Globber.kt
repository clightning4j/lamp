package com.lvaccaro.lamp.Services

import java.io.*

class Globber(val stream: InputStream, val file: File): Thread() {
    override fun run() {
        try {
            val isr = InputStreamReader(stream)
            val br = BufferedReader(isr)
            while(!Thread.currentThread().isInterrupted()) {
                val line = br.readLine()
                file.appendText(line)
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }
}