/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package htsjdk.samtools.seekablestream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper class to provide buffered read access to a SeekableStream. Just wrapping such a stream
 * with a BufferedInputStream will not work as it does not support seeking. In this implementation a
 * seek call is delegated to the wrapped stream, and the buffer reset.
 */
public class SeekableBufferedStream extends SeekableStream {

  /**
   * Little extension to buffered input stream to give access to the available bytes in the buffer.
   */
  private class ExtBufferedInputStream extends BufferedInputStream {
    private ExtBufferedInputStream(final InputStream inputStream, final int i) {
      super(inputStream, i);
    }

    /**
     * Returns the number of bytes that can be read from the buffer without reading more into the
     * buffer.
     */
    int getBytesInBufferAvailable() {
      return this.count - this.pos;
    }

    /** Return true if the position can be changed by the given delta and remain in the buffer. */
    boolean canChangePos(long delta) {
      long newPos = this.pos + delta;
      return newPos >= 0 && newPos < this.count;
    }

    /** Changes the position in the buffer by a given delta. */
    void changePos(int delta) {
      int newPos = this.pos + delta;
      if (newPos < 0 || newPos >= this.count) {
        throw new IllegalArgumentException(
            "New position not in buffer pos=" + this.pos + ", delta=" + delta);
      }
      this.pos = newPos;
    }
  }

  public static final int DEFAULT_BUFFER_SIZE = 512000;

  private final int bufferSize;
  final SeekableStream wrappedStream;
  ExtBufferedInputStream bufferedStream;
  long position;

  public SeekableBufferedStream(final SeekableStream stream, final int bufferSize) {
    this.bufferSize = bufferSize;
    this.wrappedStream = stream;
    this.position = 0;
    bufferedStream = new ExtBufferedInputStream(wrappedStream, bufferSize);
  }

  public SeekableBufferedStream(final SeekableStream stream) {
    this(stream, DEFAULT_BUFFER_SIZE);
  }

  @Override
  public long length() {
    return wrappedStream.length();
  }

  @Override
  public long skip(final long skipLength) throws IOException {
    if (skipLength < this.bufferedStream.getBytesInBufferAvailable()) {
      final long retval = this.bufferedStream.skip(skipLength);
      this.position += retval;
      return retval;
    } else {
      final long position = this.position + skipLength;
      seekInternal(position);
      return skipLength;
    }
  }

  @Override
  public void seek(final long position) throws IOException {
    if (this.position == position) {
      return;
    }
    // check if the seek is within the buffer
    long delta = position - this.position;
    if (this.bufferedStream.canChangePos(delta)) {
      // casting to an int is safe since the buffer is less than the size of an int
      this.bufferedStream.changePos((int) delta);
      this.position = position;
    } else {
      seekInternal(position);
    }
  }

  private void seekInternal(final long position) throws IOException {
    wrappedStream.seek(position);
    bufferedStream = new ExtBufferedInputStream(wrappedStream, bufferSize);
    this.position = position;
  }

  @Override
  public int read() throws IOException {
    int b = bufferedStream.read();
    position++;
    return b;
  }

  @Override
  public int read(final byte[] buffer, final int offset, final int length) throws IOException {
    int nBytesRead = bufferedStream.read(buffer, offset, length);
    if (nBytesRead > 0) {
      // if we can't read as many bytes as we are asking for then attempt another read to reset the
      // buffer.
      if (nBytesRead < length) {
        final int additionalBytesRead =
            bufferedStream.read(buffer, nBytesRead + offset, length - nBytesRead);
        // if there were additional bytes read then update nBytesRead
        if (additionalBytesRead > 0) {
          nBytesRead += additionalBytesRead;
        }
      }
      position += nBytesRead;
    }
    return nBytesRead;
  }

  @Override
  public void close() throws IOException {
    wrappedStream.close();
  }

  @Override
  public boolean eof() throws IOException {
    return position >= wrappedStream.length();
  }

  @Override
  public String getSource() {
    return wrappedStream.getSource();
  }

  @Override
  public long position() throws IOException {
    return position;
  }
}
