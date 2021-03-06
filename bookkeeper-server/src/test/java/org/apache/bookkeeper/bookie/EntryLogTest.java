/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.bookkeeper.bookie;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.bookkeeper.conf.ServerConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryLogTest extends TestCase {
    static Logger LOG = LoggerFactory.getLogger(EntryLogTest.class);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCorruptEntryLog() throws IOException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        File tmpDir = File.createTempFile("bkTest", ".dir");
        tmpDir.delete();
        tmpDir.mkdir();
        ServerConfiguration conf = new ServerConfiguration();
        conf.setLedgerDirNames(new String[] {tmpDir.toString()});
        // create some entries
        EntryLogger logger = new EntryLogger(conf, null);
        logger.addEntry(1, generateEntry(1, 1));
        logger.addEntry(3, generateEntry(3, 1));
        logger.addEntry(2, generateEntry(2, 1));
        logger.flush();
        // now lets truncate the file to corrupt the last entry, which simulates a partial write
        File f = new File(tmpDir, "0.log");
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.setLength(raf.length()-10);
        raf.close();
        // now see which ledgers are in the log
        logger = new EntryLogger(conf, null);
        Field entryLogs2LedgersMapField = logger.getClass().getDeclaredField("entryLogs2LedgersMap");
        entryLogs2LedgersMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Map<Long, Boolean>> ledgersMap = (Map<Long, Map<Long, Boolean>>) entryLogs2LedgersMapField.get(logger);
        LOG.info("LedgersMap.get(0) {}", ledgersMap.get(0L));
        assertNotNull(ledgersMap.get(0L).get(1L));
        assertNull(ledgersMap.get(0L).get(2L));
        assertNotNull(ledgersMap.get(0L).get(3L));
    }

    private ByteBuffer generateEntry(long ledger, long entry) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[64]);
        bb.putLong(ledger);
        bb.putLong(entry);
        bb.put(("ledger"+ledger).getBytes());
        bb.flip();
        return bb;
    }
    
    @After
    public void tearDown() throws Exception {
    }

}
