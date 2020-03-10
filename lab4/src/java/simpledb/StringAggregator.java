package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private HashMap<Field, Integer> counts;
    private String groupName;
    private int noGroupCounts;
    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if(what != Op.COUNT)
            throw new IllegalArgumentException();
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        counts = new HashMap<>();
        noGroupCounts = 0;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if(gbfield == NO_GROUPING){
            noGroupCounts++;
        }
        else {
            groupName = tup.getTupleDesc().getFieldName(gbfield);
            Field groupField = tup.getField(gbfield);
            if(!counts.containsKey(groupField)){
                counts.put(groupField, 1);
            }
            else {
                counts.put(groupField, counts.get(groupField) + 1);
            }
        }

    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
//        throw new UnsupportedOperationException("please implement me for lab2");
        TupleDesc td;
        Tuple t;
        List<Tuple> tuples = new ArrayList<>();
        if(gbfield == NO_GROUPING){
            td = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{what.toString()});
            t = new Tuple(td);
            t.setField(0, new IntField(noGroupCounts));
            tuples.add(t);
        }
        else {
            td = new TupleDesc(new Type[]{gbfieldtype, Type.INT_TYPE}, new String[]{groupName, what.toString()});
            for(Field g : counts.keySet()){
                t = new Tuple(td);
                t.setField(0, g);
                t.setField(1, new IntField(counts.get(g)));
                tuples.add(t);
            }
        }
        return new TupleIterator(td, tuples);
    }

}
