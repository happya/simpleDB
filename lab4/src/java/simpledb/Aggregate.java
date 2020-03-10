package simpledb;

import java.util.*;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     * 
     * 
     * @param child
     *            The OpIterator that is feeding us tuples.
     * @param afield
     *            The column over which we are computing an aggregate.
     * @param gfield
     *            The column over which we are grouping the result, or -1 if
     *            there is no grouping
     * @param aop
     *            The aggregation operator to use
     */
    private OpIterator child;
    private int afield;
    private int gfield;
    private Aggregator.Op aop;
    private OpIterator aggIter;
    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
	// some code goes here
        this.child = child;
        this.afield = afield;
        this.gfield = gfield;
        this.aop = aop;
        this.aggIter = null;
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link simpledb.Aggregator#NO_GROUPING}
     * */
    public int groupField() {
	// some code goes here
	    return gfield;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     * */
    public String groupFieldName() {
	// some code goes here
	    return gfield == Aggregator.NO_GROUPING ? null : child.getTupleDesc().getFieldName(gfield);
    }

    /**
     * @return the aggregate field
     * */
    public int aggregateField() {
	// some code goes here
	    return afield;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     * */
    public String aggregateFieldName() {
	// some code goes here
	    return child.getTupleDesc().getFieldName(afield);
    }

    /**
     * @return return the aggregate operator
     * */
    public Aggregator.Op aggregateOp() {
	// some code goes here
	    return aop;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
	return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
	    TransactionAbortedException {
	    // some code goes here
        super.open();
        TupleDesc childTd = child.getTupleDesc();
        if(aggIter == null){
            Type afieldType = childTd.getFieldType(afield);
            Aggregator agg;
            Type gfieldType = (gfield == Aggregator.NO_GROUPING ? null : childTd.getFieldType(gfield));
            // If the aggregate field has Int type
            if(afieldType.equals(Type.INT_TYPE)){
                agg = new IntegerAggregator(gfield, gfieldType, afield, aop);
            }
            // else will be string type, because we only have two types
            else {
                agg = new StringAggregator(gfield, gfieldType, afield, aop);
            }
            child.open();
            Tuple tup;
            while(child.hasNext()){
                tup = child.next();
                agg.mergeTupleIntoGroup(tup);
            }
            aggIter = agg.iterator();
        }
        aggIter.open();
    }

    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
	// some code goes here
	    if(aggIter.hasNext())
	        return aggIter.next();
	    return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
	// some code goes here
        super.close();
        aggIter.close();
        open();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * 
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
	// some code goes here
        Type[] types;
        String[] names;
        // the name of the aggregate column: aggName(aop)
        String aggColName = nameOfAggregatorOp(aop) + "(" + aggregateFieldName() + ")";
	    if(gfield == Aggregator.NO_GROUPING){
	        types = new Type[]{Type.INT_TYPE};
	        names = new String[]{aggColName};
        }
	    else {
	        Type gfieldtype = child.getTupleDesc().getFieldType(gfield);

	        types = new Type[]{gfieldtype, Type.INT_TYPE};
	        names = new String[]{groupFieldName(), aggColName};
        }
        return new TupleDesc(types, names);
    }

    public void close() {
	// some code goes here
        super.close();
        aggIter.close();
    }

    @Override
    public OpIterator[] getChildren() {
	// some code goes here
	    return new OpIterator[]{this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
	// some code goes here
        this.child = children[0];
    }
    
}
