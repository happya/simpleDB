package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */
    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;
    private HashMap<Field, Integer> groups; // (group field, value) map
    private HashMap<Field, Integer> counts; // (group field, count value) map
    private boolean hasGroup;
    private String groupName;

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        if(gbfield == Aggregator.NO_GROUPING){
            hasGroup = false;
        }
        else {
            hasGroup = true;
        }
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        groups = new HashMap<>();
        counts = new HashMap<>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here

        // get the group field and its name
        Field groupField = (gbfield == NO_GROUPING ? new IntField(0) : tup.getField(gbfield));
        groupName = (gbfield == NO_GROUPING ? null : tup.getTupleDesc().getFieldName(gbfield));

        IntField aggField = (IntField)tup.getField(afield);
        int aggValue = aggField.getValue();

        if(!counts.containsKey(groupField)){
            counts.put(groupField, 1);
            groups.put(groupField, aggValue);
        }

        else {
            counts.put(groupField, counts.get(groupField) + 1);
            int oldValue = groups.get(groupField);
            switch (what){
                case AVG:
                case SUM_COUNT:
                case SUM:
                    groups.put(groupField, oldValue + aggValue);
                    break;
                case MAX:
                    groups.put(groupField, Math.max(oldValue, aggValue));
                    break;
                case MIN:
                    groups.put(groupField, Math.min(oldValue, aggValue));
                    break;
                case COUNT:
                default:
                    break;
            }
        }
    }
    private int[] getAggResults(Field group){
        switch (what){
            case COUNT:
                return new int[]{counts.get(group)};
            case MAX:
            case MIN:
            case SUM:
                return new int[]{groups.get(group)};
            case AVG:
//            case SC_AVG:
                return new int[]{groups.get(group) / counts.get(group)};
            case SUM_COUNT:
                return new int[]{groups.get(group), counts.get(group)};
            default:
                break;
        }
        return new int[]{};
    }
    /**
     * Create a OpIterator over group aggregate results.
     * 
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
//        throw new
//        UnsupportedOperationException("please implement me for lab2");
        TupleDesc td;
        Type[] types;
        String[] names;
        if(hasGroup){
            if(what == Op.SUM_COUNT){
                types = new Type[]{gbfieldtype, Type.INT_TYPE, Type.INT_TYPE};
                names = new String[]{groupName, Op.SUM.toString(), Op.COUNT.toString()};
            }
            else{
                types = new Type[]{gbfieldtype, Type.INT_TYPE};
                names = new String[]{groupName, what.toString()};
            }
        }
        else {
            if(what == Op.SUM_COUNT) {
                types = new Type[]{Type.INT_TYPE, Type.INT_TYPE};
                names = new String[]{Op.SUM.toString(), Op.COUNT.toString()};
            }
            else {
                types = new Type[]{Type.INT_TYPE};
                names = new String[]{what.toString()};
            }
        }
        td = new TupleDesc(types, names);
        List<Tuple> tuples = new ArrayList<>();
        for(Field g : counts.keySet()){
            Tuple t = new Tuple(td);
            if(hasGroup){
                t.setField(0, g);
            }
            t.setField(hasGroup ? 1 : 0, new IntField(getAggResults(g)[0]));
            if(what == Op.SUM_COUNT){
                t.setField(hasGroup ? 2 : 1, new IntField(getAggResults(g)[1]));
            }
            tuples.add(t);
        }
        return new TupleIterator(td, tuples);
    }

}
