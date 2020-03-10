package simpledb;

import java.util.*;

public class LockManager {
    private Map<PageId, LockState> pageLocksMap;
    private HashMap<TransactionId, HashSet<PageId>> transLocks; // all transaction locks, (tid, (pids)) pair

    // locks dependency graph
    private HashMap<TransactionId, HashSet<TransactionId>> locksGraph;


    public LockManager(){
        transLocks = new HashMap<>();
//        readLocks = new HashMap<>();
//        writeLocks = new HashMap<>();
        locksGraph = new HashMap<>();
        pageLocksMap = new HashMap<>();
    }

    /**
     * Return true if such tid has a readLock or writeLock on a certain page.
     * @param tid: transaction id
     * @param pid: page id of the request page
     * @return true: if this tid has read/write lock on page.
     */

    public synchronized boolean holdsLock(TransactionId tid, PageId pid){
        return transLocks.containsKey(tid) && transLocks.get(tid).contains(pid);
    }

    /**
     * acquire the grant of a read lock (tid) on a specified page
     * @param tid: lock id need to be grant
     * @param pid: page id that is requested
     * @throws TransactionAbortedException
     */
    public void acquireReadLock(TransactionId tid, PageId pid)
        throws TransactionAbortedException {
        LockState locksOnPage = getOrCreateLockState(pid);
        synchronized (this){
            if(locksOnPage.isHoldBy(tid))
                return;

            // if this page has write locks
            // add them as the nodes connected to current tid
            if(locksOnPage.hasLocks() && locksOnPage.isExclusive()){
                locksGraph.put(tid, locksOnPage.allLocksOnPage());

                // if the current tid will cause deadlock
                // remove this tid
                if(hasDeadLock(tid)){
                    locksGraph.remove(tid);
                    throw new TransactionAbortedException();
                }
            }

        }
        locksOnPage.addReadLock(tid); // add read lock to this page
        synchronized (this){
            locksGraph.remove(tid); // remove this tid in the locks graph
            addLockedPages(tid, pid); // mark this page as locked by this tid
        }

    }

    /**
     * acquire the grant of a write lock on a specified page
     * @param tid: lock id need to be grant
     * @param pid: page id that is requested
     * @throws TransactionAbortedException
     */
    public void acquireWriteLock(TransactionId tid, PageId pid)
        throws TransactionAbortedException {
        LockState locksOnPage = getOrCreateLockState(pid);
        synchronized (this){
            // if this tid is already a write lock on this page, do nothing
            if(locksOnPage.isExclusive() && locksOnPage.isHoldBy(tid))
                return;
            // if this page already held by locks, check for deadlock
            if(locksOnPage.hasLocks()){
                locksGraph.put(tid, locksOnPage.allLocksOnPage());
                if(hasDeadLock(tid)){
                    locksGraph.remove(tid);
                    throw new TransactionAbortedException();
                }
            }
        }
        // add write lock to this page
        locksOnPage.addWriteLock(tid);

        synchronized (this){
            locksGraph.remove(tid);
            addLockedPages(tid, pid);
        }
    }


    /**
     * helper dfs to detect cycles in the lock graph
     * @param tid: start graph node
     * @param visited: visited nodes record
     * @return true if detect cycle
     */
    private boolean dfs(TransactionId tid, HashSet<TransactionId> visited){
        if(locksGraph.containsKey(tid)){
            visited.add(tid);
            for(TransactionId w : locksGraph.get(tid)){
                // not count self-loops
                if(!w.equals(tid)){
                    if(!visited.contains(w)){
                        if(dfs(w, visited))
                            return true;
                    }
                    // if this node is visited, cycle exists
                    else
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * return true if exists cycle starting with the current tid
     * @param tid
     * @return true if detected a cycle
     */
    private boolean hasDeadLock(TransactionId tid){
        HashSet<TransactionId> visited = new HashSet<>();
        Queue<TransactionId> q = new LinkedList<>();

        // dfs to find circle
        return dfs(tid, visited);

        // bfs to find circle
//        visited.add(tid);
//        q.offer(tid);
//
//        while(!q.isEmpty()){
//            TransactionId cur = q.poll();
//            if(LockGraph.containsKey(cur)){
//                for(TransactionId w : LockGraph.get(cur)){
//                    if(w.equals(cur))
//                        continue;
//                    if(!visited.contains(w)){
//                        visited.add(w);
//                        q.offer(w);
//                    }
//                    else{
//                        System.out.println("detect dead lock");
//                        return true;
//                    }
//                }
//            }
//
//        }
    }

    /**
     * get LockState for a given page
     * if this pid has not been added, create and return a new lock-state
     * else return the lock-state
     * @param pid: page id
     * @return
     */
    private LockState getOrCreateLockState(PageId pid){
        if(!pageLocksMap.containsKey(pid)){
            pageLocksMap.put(pid, new LockState());
        }
        return pageLocksMap.get(pid);
    }

    /**
     * add page id which is locked by the tid
     * @param tid: transaction id in pageLockMap
     * @param pid: page id that is locked by tid
     */
    private void addLockedPages(TransactionId tid, PageId pid){
        if(!transLocks.containsKey(tid)){
            transLocks.put(tid, new HashSet<>());
        }
        transLocks.get(tid).add(pid);
    }

    /**
     * get page ids that are locked by the specified tid
     * @param tid
     * @return
     */
    public HashSet<PageId> getLockedPages(TransactionId tid){
        if(transLocks.containsKey(tid))
            return transLocks.get(tid);
        return null;
    }

    /**
     * release lock on a given page with given tid
     * and remove from transLock map
     * @param tid
     * @param pid
     */
    public synchronized void releaseLock(TransactionId tid, PageId pid){
        if(pageLocksMap.containsKey(pid)){
            pageLocksMap.get(pid).unlockAll(tid);
            transLocks.get(tid).remove(pid);
        }
    }

    /**
     * unlock tid on a given page
     * @param tid
     * @param pid
     */
    private synchronized void removeLock(TransactionId tid, PageId pid){
        if(pageLocksMap.containsKey(pid)){
            pageLocksMap.get(pid).unlockAll(tid);
        }
    }
    /**
     * release all locks with a given tid
     * remove related read/write locks, and then remove from transLock map
     * @param tid
     */
    public synchronized void releaseAllLocks(TransactionId tid){
        if(transLocks.containsKey(tid) && !transLocks.get(tid).isEmpty()){
            for(PageId pid : transLocks.get(tid)){
                removeLock(tid, pid);
            }
            // remove from transLock map
            transLocks.remove(tid);
        }
    }




    //    private HashMap<PageId, HashSet<TransactionId>> readLocks; // shared locks
//    private HashMap<PageId, TransactionId> writeLocks; // exclusive locks

//    private void addLock(TransactionId tid, PageId pid, Permissions perm){
//        if(perm == Permissions.READ_ONLY){
//            if(!readLocks.containsKey(pid)){
//                readLocks.put(pid, new HashSet<>());
//            }
//            readLocks.get(pid).add(tid);
//        }
//        else {
//            writeLocks.put(pid, tid);
//        }
//        if(!transLocks.containsKey(tid)){
//            transLocks.put(tid, new HashSet<>());
//        }
//        transLocks.get(tid).add(pid);
//    }


//    public synchronized boolean acquireLock(TransactionId tid, PageId pid, Permissions perm){
//        if(holdsLock(tid, pid, perm)){
//            return true;
//        }
//
//        // wait for the lock to become available
//        long start = System.currentTimeMillis();
//        long timeout = (long)(Math.random() * 3000) + 5000;
//        while(!canAcquireLock(tid, pid, perm)){
//            // catch interruption 5 times every second
//            try{
//                wait(100);
//            }
//            catch (InterruptedException e){
//                e.printStackTrace();
//                return false;
//            }
//            // if times out, return false, not able to acquire that lock
//            long elapse = System.currentTimeMillis() - start;
//
//            if(elapse > timeout){
//                return false;
//            }
//
//        }
//        // add lock
//        addLock(tid, pid, perm);
//
//
//        return true;
//    }
//
//
//
//    public synchronized boolean canAcquireLock(TransactionId tid, PageId pid, Permissions perm){
//        if(holdsLock(tid, pid, perm)){
//            return true;
//        }
//        // flag: whether this page has read/write locks
//        boolean hasReadLock = readLocks.containsKey(pid);
//        boolean hasWriteLock = writeLocks.containsKey(pid);
//        // if a page has no lock, return true
//        if((!hasReadLock && !hasWriteLock))
//            return true;
//
//        // if no write-lock on this page
//        // if the current request is read, return true;
//        // if the current request is read-write, check if it can be granted
//        if(!hasWriteLock){
//            if(perm == Permissions.READ_ONLY){ return true; }
//            else if(canUpgradeTid(tid, pid)){
//                return true;
//            }
//        }
//        // false for all the other cases
//        return false;
//    }
//
//
//    /*
//        check whether a tid on a given page can be upgraded
//        if such tid exists, and it is the only read lock on this page, it can be upgraded to a write lock
//     */
//    private synchronized boolean canUpgradeTid(TransactionId tid, PageId pid){
//        return transLocks.containsKey(tid) && transLocks.get(tid).contains(pid)
//                && readLocks.containsKey(pid) && readLocks.get(pid).contains(tid) && (readLocks.get(pid).size() == 1)
//                && (!writeLocks.containsKey(pid));
//    }

    /**
     * remove given tid in given pages (write lock and read lock)
     * remove from page
     * @param tid
     * @param pid
     */
//    public synchronized void removeLock(TransactionId tid, PageId pid){
//        // remove read lock
//        if(readLocks.containsKey(pid)){
//            readLocks.get(pid).remove(tid);
//            if(readLocks.get(pid).isEmpty()){
//                readLocks.remove(pid);
//            }
//        }
//        // remove write lock
//        // only one tid can write at a time
//        if(writeLocks.containsKey(pid) && writeLocks.get(pid).equals(tid)){
//            writeLocks.remove(pid);
//        }
//    }

    /**
     * Return true if such tid has a readLock or writeLock on a certain page for the assigend
     * permission type.
     * @param tid
     * @param pid
     * @param perm
     * @return
     */

//    public synchronized boolean holdsLock(TransactionId tid, PageId pid, Permissions perm){
//        if(perm == Permissions.READ_ONLY){
//            // if it's read-only
//            // return true if it has corresponding read lock
//            // or this tid is a write lock on this page
//            return (readLocks.containsKey(pid) && readLocks.get(pid).contains(tid))
//                    || (writeLocks.containsKey(pid) && writeLocks.get(pid).equals(tid));
//        } else {
//            // if it's read-write
//            // return true only if this tid is a write lock on this page
//            return writeLocks.containsKey(pid) && writeLocks.get(pid).equals(tid);
//        }
//    }


}

