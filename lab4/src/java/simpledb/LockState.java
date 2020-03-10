package simpledb;

import java.util.*;

/**
 * a class that manage all the locks on a page
 */
public class LockState {
    private HashSet<TransactionId> holdLocks; // all locks held by the page
    private int readCount; // number of read locks
    private int writeCount; // number of write locks

    private boolean isExclusive; // if has write lock, is exclusive


    // locks that requests lock on this page
    // true: write lock request; false read lock request:
    private HashMap<TransactionId, Boolean> acquireLocks;
    public LockState(){
        holdLocks = new HashSet<>();
        acquireLocks = new HashMap<>();
        isExclusive = false;
        readCount = 0;
        writeCount = 0;
    }

    /**
     * add a read lock to this page specified tid
     * @param tid: tid of the read lock
     */

    public void addReadLock(TransactionId tid){
        // if this page already has this tid and only has read locks
        if(holdLocks.contains(tid) && !isExclusive)
            return;

        acquireLocks.put(tid, false);// one read lock is acquiring
        synchronized (this){
            try{
                // if this page has write locks, wait to have no write locks
                while(writeCount > 0)
                    wait();
                addLock(tid, false);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        acquireLocks.remove(tid);
    }

    /**
     * add write lock to this page with specified tid
     * @param tid: tid of the write lock
     */
    public void addWriteLock(TransactionId tid){
        // already exists, return
        if(holdLocks.contains(tid) && isExclusive)
            return;
        // if current tid has already been acquired as a write lock
        if (acquireLocks.containsKey(tid) && acquireLocks.get(tid))
            return;

        acquireLocks.put(tid, true); // one write lock is acquiring
        synchronized (this){
            try{
                // if current tid exists as a read lock
                // wait until only this read lock exist
                // then it can upgrade to write-lock
                if(holdLocks.contains(tid)){
                    while(holdLocks.size() > 1)
                        wait();
                    // unlock this read lock to do upgrade
                    unlockRead(tid, false);
                }
                while(readCount > 0 || writeCount > 0)
                    wait();

                addLock(tid, true);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        acquireLocks.remove(tid);
    }

    /**
     * helper method to do the add read/write lock
     * @param tid: tid of the lock
     * @param isWrite: is write lock or not
     */
    private void addLock(TransactionId tid, boolean isWrite){
        holdLocks.add(tid);
        isExclusive = isWrite;
        if(isWrite) writeCount++;
        else readCount++;
    }

    /**
     * unlock a read lock, and whether to notify all
     * @param tid: the lock to be unlocked
     * @param toNotifyAll: whether to notifyall or not
     */
    public void unlockRead(TransactionId tid, boolean toNotifyAll){
        if(holdLocks.contains(tid)){
            synchronized (this){
                readCount--;
                holdLocks.remove(tid);
                if(toNotifyAll){
                    notifyAll();
                }
            }
        }
    }

    /**
     * unlock a write lock, and whether to notify all
     * @param tid: the lock to be unlocked
     * @param toNotifyAll: whether to notifyall or not
     */
    public void unlockWrite(TransactionId tid, boolean toNotifyAll){
        if(holdLocks.contains(tid) && isExclusive){
            synchronized (this){
                writeCount--;
                holdLocks.remove(tid);
                if(toNotifyAll){
                    notifyAll();
                }
            }
        }
    }

    /**
     * unlock a lock without specified read or write, if has write lock on this page, then unlock write.
     * @param tid: the lock to be unlocked
     */
    public void unlockAll(TransactionId tid){
        if(isExclusive)
            unlockWrite(tid, true);
        else
            unlockRead(tid, true);
    }

    /**
     * return true if this page is locked by a given tid
     * @param tid
     * @return
     */
    public boolean isHoldBy(TransactionId tid){
        return holdLocks.contains(tid);
    }

    /**
     * return true if the page is locked
     * @return
     */
    public boolean hasLocks(){
        return !holdLocks.isEmpty();
    }

    /**
     * @return all locks on this page
     */
    public HashSet<TransactionId> allLocksOnPage(){
        return holdLocks;
    }

    /**
     * @return true if this page has write lock
     */
    public boolean isExclusive() {
        return isExclusive;
    }

    /**
     * remove all locks on that page
     */
    public void removeAllLocks(){
        for(TransactionId tid : holdLocks){
            unlockAll(tid);
        }
    }

}
