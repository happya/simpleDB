package simpledb;

public class Table {
    private String name;
    private String pkeyField;
    private DbFile file;

    public Table(DbFile file, String name, String pkeyField) {
        this.file = file;
        this.name = name;
        this.pkeyField = pkeyField;
    }
    public DbFile getFile() { return this.file; }
    public String getName() { return this.name; }
    public String getPkeyField() { return this.pkeyField; }
}
