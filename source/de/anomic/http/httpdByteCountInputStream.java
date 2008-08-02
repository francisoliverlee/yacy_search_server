//httpByteCountinputStream.java 
//-----------------------
//(C) by Michael Peter Christen; mc@yacy.net
//first published on http://www.anomic.de
//Frankfurt, Germany, 2004
//
// This file is contributed by Martin Thelian
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package de.anomic.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class httpdByteCountInputStream extends FilterInputStream {
    
    private static final Object syncObject = new Object();
    private static final HashMap<String, Long> byteCountInfo = new HashMap<String, Long>(2);
    private static long globalByteCount = 0;
    
    private boolean finished = false;
    protected long byteCount;
    private String byteCountAccountName = null; 

    protected httpdByteCountInputStream(final InputStream inputStream) {
        this(inputStream,null);
    }
    
    /**
     * Constructor of this class
     * @param inputStream the {@link InputStream} to read from
     */
    public httpdByteCountInputStream(final InputStream inputStream, final String accountName) {
        this(inputStream,0,accountName);
    }
    
    /**
     * Constructor of this class
     * @param inputStream the {@link InputStream} to read from
     * @param initByteCount to initialize the bytecount with a given value
     */
    public httpdByteCountInputStream(final InputStream inputStream, final int initByteCount, final String accountName) {
        super(inputStream);
        this.byteCount = initByteCount;
        this.byteCountAccountName = accountName;
    }  
    
    public int read(final byte[] b) throws IOException {
        final int readCount = super.read(b);
        if (readCount > 0) this.byteCount += readCount;
        return readCount;
    }

    public int read(final byte[] b, final int off, final int len) throws IOException {
        try {
        final int readCount = super.read(b, off, len);
        if (readCount > 0) this.byteCount += readCount;
        return readCount;
        } catch (final IOException e) {
            throw new IOException(e.getMessage() + "; b.length = " + b.length + ", off = " + off + ", len = " + len);
        }
    }

    public int read() throws IOException {
        this.byteCount++;
        return super.read();
    }
    
    public long skip(final long len) throws IOException {
        final long skipCount = super.skip(len);
        if (skipCount > 0) this.byteCount += skipCount; 
        return skipCount;
    }

    public long getCount() {
        return this.byteCount;
    }
    
    public String getAccountName() {
        return this.byteCountAccountName;
    }
    
    public static long getGlobalCount() {
        synchronized (syncObject) {
            return globalByteCount;
        }
    }
    
    public static long getAccountCount(final String accountName) {
        synchronized (syncObject) {
            if (byteCountInfo.containsKey(accountName)) {
                return (byteCountInfo.get(accountName)).longValue();
            }
            return 0;
        }
    }
    
    public void close() throws IOException {
        super.close();
        this.finish();
    }
    
    public void finish() {
        if (this.finished) return;
        
        this.finished = true;
        synchronized (syncObject) {
            globalByteCount += this.byteCount;
            if (this.byteCountAccountName != null) {
                long lastByteCount = 0;
                if (byteCountInfo.containsKey(this.byteCountAccountName)) {
                    lastByteCount = byteCountInfo.get(this.byteCountAccountName).longValue();
                }
                lastByteCount += this.byteCount;
                byteCountInfo.put(this.byteCountAccountName,new Long(lastByteCount));
            }
            
        }        
    }
    
    public static void resetCount() {
        synchronized (syncObject) {
            globalByteCount = 0;
            byteCountInfo.clear();
        }
    }
    
    protected void finalize() throws Throwable {
        if (!this.finished) 
            finish();
        super.finalize();
    }
}
