/*
* Copyright 2014 The Netty Project
*
* The Netty Project licenses this file to you under the Apache License,
* version 2.0 (the "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at:
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.FileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import static org.junit.Assert.*;

public class HttpResponseEncoderTest {
    private static final long INTEGER_OVERLFLOW = (long) Integer.MAX_VALUE + 1;
    private static final FileRegion FILE_REGION = new DummyLongFileRegion();

    @Test
    public void testLargeFileRegionChunked() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpResponseEncoder());
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        assertTrue(channel.writeOutbound(response));

        ByteBuf buffer = channel.readOutbound();

        assertEquals("HTTP/1.1 200 OK\r\n" + HttpHeaders.Names.TRANSFER_ENCODING + ": " +
                HttpHeaders.Values.CHUNKED + "\r\n\r\n", buffer.toString(CharsetUtil.US_ASCII));
        buffer.release();
        assertTrue(channel.writeOutbound(FILE_REGION));
        buffer = channel.readOutbound();
        assertEquals("80000000\r\n", buffer.toString(CharsetUtil.US_ASCII));
        buffer.release();

        FileRegion region = channel.readOutbound();
        assertSame(FILE_REGION, region);
        region.release();
        buffer = channel.readOutbound();
        assertEquals("\r\n", buffer.toString(CharsetUtil.US_ASCII));
        buffer.release();

        assertTrue(channel.writeOutbound(LastHttpContent.EMPTY_LAST_CONTENT));
        buffer = channel.readOutbound();
        assertEquals("0\r\n\r\n", buffer.toString(CharsetUtil.US_ASCII));
        buffer.release();

        assertFalse(channel.finish());
    }

    private static class DummyLongFileRegion implements FileRegion {

        @Override
        public long position() {
            return 0;
        }

        @Override
        public long transfered() {
            return 0;
        }

        @Override
        public long count() {
            return INTEGER_OVERLFLOW;
        }

        @Override
        public long transferTo(WritableByteChannel target, long position) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileRegion touch(Object hint) {
            return this;
        }

        @Override
        public FileRegion touch() {
            return this;
        }

        @Override
        public FileRegion retain() {
            return this;
        }

        @Override
        public FileRegion retain(int increment) {
            return this;
        }

        @Override
        public int refCnt() {
            return 1;
        }

        @Override
        public boolean release() {
            return false;
        }

        @Override
        public boolean release(int decrement) {
            return false;
        }
    }
}
