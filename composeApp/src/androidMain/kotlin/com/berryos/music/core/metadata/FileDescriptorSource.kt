package com.berryos.music.core.metadata

import android.os.ParcelFileDescriptor
import android.system.Os
import com.berryos.audiometadata.io.WritableRandomAccessSource
import java.io.FileDescriptor

/** [WritableRandomAccessSource] over a file descriptor owned by the caller. */
internal class FileDescriptorSource(descriptor: ParcelFileDescriptor) : WritableRandomAccessSource {
    private val fd: FileDescriptor = descriptor.fileDescriptor
    override val size: Long
        get() = Os.fstat(fd).st_size

    override fun readAt(offset: Long, destination: ByteArray, destinationOffset: Int, length: Int): Int {
        require(offset >= 0) { "offset must not be negative" }
        require(destinationOffset >= 0 && length >= 0 && destinationOffset + length <= destination.size) {
            "invalid destination range"
        }
        if (length == 0) return 0
        var total = 0
        while (total < length) {
            val read = Os.pread(fd, destination, destinationOffset + total, length - total, offset + total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    override fun writeAt(offset: Long, source: ByteArray, sourceOffset: Int, length: Int) {
        require(offset >= 0) { "offset must not be negative" }
        require(sourceOffset >= 0 && length >= 0 && sourceOffset + length <= source.size) {
            "invalid source range"
        }
        var total = 0
        while (total < length) {
            val written = Os.pwrite(fd, source, sourceOffset + total, length - total, offset + total)
            if (written <= 0) error("Unable to write at ${offset + total}")
            total += written
        }
    }

    override fun truncate(size: Long) {
        Os.ftruncate(fd, size)
    }

    override fun flush() {
        Os.fsync(fd)
    }

    override fun close() = Unit
}
