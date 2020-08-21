package com.lvaccaro.lamp.utils

import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class Archive {

    fun uncompressXZ(inputFile: File, outputDir: File) {
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }
        val pluginsDir = File(outputDir, "plugins")
        if (!pluginsDir.exists()) {
            pluginsDir.mkdir()
        }
        val input = TarArchiveInputStream(
            BufferedInputStream(
                XZCompressorInputStream(
                    BufferedInputStream(FileInputStream(inputFile))
                )
            )
        )
        var counter = 0
        var entry = input.nextEntry
        while (entry != null) {

            val name = entry.name
            val f = File(outputDir, name)

            var out = FileOutputStream(f)
            try {
                IOUtils.copy(input, out)
            } finally {
                IOUtils.closeQuietly(out)
            }

            val mode = (entry as TarArchiveEntry).mode
            //noinspection ResultOfMethodCallIgnored
            f.setExecutable(true, mode and 1 == 0)
            entry = input.nextEntry
            counter++
        }
        IOUtils.closeQuietly(input)
        inputFile.delete()
    }
}
