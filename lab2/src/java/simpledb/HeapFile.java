package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {
    private TupleDesc td;
    private File file;
    private int fileId;

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.td = td;
        this.file = f;
        fileId = f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        // return file.getAbsoluteFile().hashCode();
        // make file id the class attribute
        return fileId;
        // throw new UnsupportedOperationException("implement this");
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
        // throw new UnsupportedOperationException("implement this");
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        try {
            RandomAccessFile rafReader = new RandomAccessFile(file, "r");
            int pageNumber = pid.getPageNumber();
            int bytesPerPage = BufferPool.getPageSize();
            // calculate the offset of bytes of the page to be read
            int offset = pageNumber * bytesPerPage;
            byte[] pageData = new byte[bytesPerPage];
            // seek the page with given offset
            rafReader.seek(offset);
            // read this page and store in PageData
            rafReader.read(pageData, 0, bytesPerPage);

            rafReader.close();
            return new HeapPage(new HeapPageId(pid.getTableId(), pageNumber), pageData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException();
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        try{
            RandomAccessFile rafRw = new RandomAccessFile(file, "rw");
            int bytesPerPage = BufferPool.getPageSize();

            rafRw.seek(bytesPerPage * page.getId().getPageNumber());
//            byte[] pageData = new byte[bytesPerPage];
//            pageData = page.getPageData();
            rafRw.write(page.getPageData(), 0, bytesPerPage);
            rafRw.close();
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return (int) Math.ceil(file.length() / (double) BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
//        return null;
        // not necessary for lab1
        ArrayList<Page> res = new ArrayList<>();
        for(int i = 0; i < numPages(); i++){
            PageId pid = new HeapPageId(getId(), i);
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);

            // if the current page has empty slots, then insert in this page
            if(page.getNumEmptySlots() > 0){
                page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
                page.insertTuple(t);
                res.add(page);
                return res;
            }
        }
        // no empty page, now to create new page
        HeapPageId pid = new HeapPageId(getId(), numPages());
        HeapPage page = new HeapPage(pid, HeapPage.createEmptyPageData());
        page.insertTuple(t);
        writePage(page);
        res.add(page);
        return res;


    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
//        return null;
        // not necessary for lab1
        if(t.getRecordId() == null)
            throw new DbException("rid is invalid, cannot delete tuple");
        // delete tuple with given valid rid
        PageId pid = t.getRecordId().getPageId();
        ArrayList<Page> res = new ArrayList<>();
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
        page.deleteTuple(t);
        res.add(page);
        return res;


    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid, this);
    }

}

