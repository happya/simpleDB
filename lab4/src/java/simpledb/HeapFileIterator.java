package simpledb;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.*;

public class HeapFileIterator implements DbFileIterator{
    private HeapFile file;
    private TransactionId tid;

    private int pageNumber; // track the page number needs to retrieve
    private Iterator<Tuple> itt;

    public HeapFileIterator(TransactionId tid, HeapFile file){
        this.file = file;
        this.tid = tid;
    }
    /**
     * Opens the iterator
     * @throws DbException when there are problems opening/accessing the database.
     */
    public void open() throws DbException, TransactionAbortedException{
        this.pageNumber = 0;
        itt = getPageIter(pageNumber);
    }

    /**
     * return a list of tuples (not empty) from a page
     * @param pageNo: page number
     * @return
     */
    private Iterator<Tuple> getPageIter(int pageNo) throws DbException, TransactionAbortedException{
        if(pageNo < 0 || pageNo >= file.numPages()){
            return null;
        }
        // construct page id
        PageId pid = new HeapPageId(file.getId(), pageNo);
        // get page from buffer pool
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
        return page.iterator();
//        List<Tuple> tupleList = new ArrayList<>();
//        Iterator<Tuple> it = pg.iterator();
//        while(it.hasNext()) {
//            tupleList.add(it.next());
//        }
//        return tupleList;
    }

    /**
     *
     * @return true: if there are more tuples available,
     *         false: if no more tuples or iterator isn't open.
     * @throws DbException
     * @throws TransactionAbortedException
     */
    public boolean hasNext() throws DbException, TransactionAbortedException{
        if(itt == null) {
            return false;
        }
        if(itt.hasNext()) {
            return true;
        }
        // if itt.hasNext() == false
        // we continue to read next Page
        else if (pageNumber >= file.numPages() - 1) {
            return false;
        } else {
            // important!
            // should correctly define hasNext()
            // itt should both be not null and also hasNext()
            // should not ignore the condiction of itt.hasNext()
            itt = getPageIter(++pageNumber);
            return (itt != null && itt.hasNext());
        }
    }


    /**
     * Gets the next tuple from the operator (typically implementing by reading
     * from a child operator or an access method).
     *
     * @return The next tuple in the iterator.
     * @throws NoSuchElementException if there are no more tuples
     */
    public Tuple next()
            throws DbException, TransactionAbortedException, NoSuchElementException {
        if(hasNext())
            return itt.next();
        throw new NoSuchElementException("no more tuples available");

    }

    /**
     * Resets the iterator to the start.
     * @throws DbException When rewind is unsupported.
     */
    public void rewind() throws DbException, TransactionAbortedException {
        close();
        open();
    }

    /**
     * Closes the iterator.
     */
    public void close() {
        itt = null;
    }


}
