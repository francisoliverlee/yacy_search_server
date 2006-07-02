package de.anomic.index;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import de.anomic.kelondro.kelondroCollectionIndex;
import de.anomic.kelondro.kelondroNaturalOrder;
import de.anomic.kelondro.kelondroRow;

public class indexCollectionRI extends indexAbstractRI implements indexRI {

    kelondroCollectionIndex collectionIndex;
    
    public indexCollectionRI(File path, String filenameStub, long buffersize) throws IOException {
        kelondroRow rowdef = new kelondroRow(new int[]{});
        
        collectionIndex = new kelondroCollectionIndex(
                path, filenameStub, 9 /*keyLength*/,
                kelondroNaturalOrder.naturalOrder, buffersize,
                1 /*loadfactor*/, rowdef, 8 /*partitions*/);
    }
    
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Iterator wordHashes(String startWordHash, boolean rot) {
        // TODO Auto-generated method stub
        return null;
    }

    public indexContainer getContainer(String wordHash, boolean deleteIfEmpty, long maxtime) {
        // TODO Auto-generated method stub
        return null;
    }

    public indexContainer deleteContainer(String wordHash) {
        // TODO Auto-generated method stub
        return null;
    }

    public int removeEntries(String wordHash, String[] referenceHashes, boolean deleteComplete) {
        // TODO Auto-generated method stub
        return 0;
    }

    public indexContainer addEntries(indexContainer newEntries, long creationTime, boolean dhtCase) {
        // TODO Auto-generated method stub
        return null;
    }

    public void close(int waitingSeconds) {
        // TODO Auto-generated method stub
        
    }



}
