package com.lvaccaro.lamp.utils

import android.os.Build
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.*

class Archive {

    companion object {
        const val RELEASE = "v0.10.0_miami"

        fun arch(): String {
            var abi: String?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                abi = Build.SUPPORTED_ABIS[0]
            } else {
                abi = Build.CPU_ABI
            }
            when (abi) {
                "armeabi-v7a" -> return "arm-linux-androideabi"
                "arm64-v8a" -> return "aarch64-linux-android"
                "x86" -> return "i686-linux-android"
                "x86_64" -> return "x86_64-linux-android"
            }
            throw Error("No arch found")
        }

        fun tarFilename(): String {
            val ARCH = arch()
            val PACKAGE = "lightning_ndk"
            return "${ARCH}_${PACKAGE}.tar.xz"
        }

        fun url(): String {
            val TAR_FILENAME = tarFilename()
            //return "https://github.com/lightningamp/lightning_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
            return "https://github.com/vincenzopalazzo/lightning_ndk/releases/download/${RELEASE}/${TAR_FILENAME}"
        }

        fun delete(downloadDir: File): Boolean {
            return File(downloadDir, tarFilename()).delete()
        }

        fun deleteUncompressed(dir: File): Boolean {
            return File(dir, "cli").deleteRecursively() &&
                    File(dir, "lightningd").deleteRecursively() &&
                    File(dir, "plugins").deleteRecursively() &&
                    File(dir, "bitcoin-cli").delete() &&
                    File(dir, "bitcoind").delete() &&
                    File(dir, "tor").delete()
        }

        fun uncompressXZ(inputFile: File, outputDir: File) {
            mkdir(outputDir)
            mkdir(File(outputDir, "plugins"))
            mkdir(File(outputDir, "lightningd"))
            mkdir(File(outputDir, "cli"))
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
                val file: File?
                file = File(outputDir, name)
                val out = FileOutputStream(file)
                try {
                    IOUtils.copy(input, out)
                } finally {
                    IOUtils.closeQuietly(out)
                }
                val mode = (entry as TarArchiveEntry).mode
                //noinspection ResultOfMethodCallIgnored
                file.setExecutable(true, mode and 1 == 0)
                entry = input.nextEntry
                counter++
            }
            IOUtils.closeQuietly(input)
            inputFile.delete()
        }

        private fun mkdir(dir: File) {
            if (!dir.exists()) {
                dir.mkdir()
            }
        }
    }
}
