package db;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Custom database implementation from scratch
 * Supports tables, indexes, transactions, and persistence
 */
public class Database {

    private final String dbPath;
    private final Map<String, Table> tables;
    private final ReadWriteLock lock;
    private boolean autoCommit;

    public Database(String dbPath) {
        this.dbPath = dbPath;
        this.tables = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.autoCommit = true;

        // Ensure database directory exists
        createDatabaseDirectory();

        // Load existing tables
        loadTables();
    }

    /**
     * Create a new table
     */
    public boolean createTable(String tableName, List<Column> columns) {
        lock.writeLock().lock();
        try {
            if (tables.containsKey(tableName)) {
                return false; // Table already exists
            }

            Table table = new Table(tableName, columns);
            tables.put(tableName, table);

            if (autoCommit) {
                saveTable(table);
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Drop a table
     */
    public boolean dropTable(String tableName) {
        lock.writeLock().lock();
        try {
            Table table = tables.remove(tableName);
            if (table == null) {
                return false;
            }

            if (autoCommit) {
                deleteTableFile(tableName);
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Insert a record into a table
     */
    public boolean insert(String tableName, Map<String, Object> values) {
        lock.writeLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return false;
            }

            Record record = new Record(values);
            boolean success = table.insert(record);

            if (success && autoCommit) {
                saveTable(table);
            }

            return success;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Select records from a table
     */
    public List<Record> select(String tableName, String whereClause) {
        lock.readLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return new ArrayList<>();
            }

            return table.select(whereClause);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Select all records from a table
     */
    public List<Record> selectAll(String tableName) {
        return select(tableName, null);
    }

    /**
     * Update records in a table
     */
    public int update(String tableName, Map<String, Object> values, String whereClause) {
        lock.writeLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return 0;
            }

            int updated = table.update(values, whereClause);

            if (updated > 0 && autoCommit) {
                saveTable(table);
            }

            return updated;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Delete records from a table
     */
    public int delete(String tableName, String whereClause) {
        lock.writeLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return 0;
            }

            int deleted = table.delete(whereClause);

            if (deleted > 0 && autoCommit) {
                saveTable(table);
            }

            return deleted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Create an index on a table
     */
    public boolean createIndex(String tableName, String columnName) {
        lock.writeLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return false;
            }

            boolean success = table.createIndex(columnName);

            if (success && autoCommit) {
                saveTable(table);
            }

            return success;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Begin a transaction
     */
    public void beginTransaction() {
        lock.writeLock().lock();
        autoCommit = false;
    }

    /**
     * Commit a transaction
     */
    public void commit() {
        try {
            // Save all tables
            for (Table table : tables.values()) {
                saveTable(table);
            }
        } finally {
            autoCommit = true;
            lock.writeLock().unlock();
        }
    }

    /**
     * Rollback a transaction
     */
    public void rollback() {
        try {
            // Reload all tables from disk
            loadTables();
        } finally {
            autoCommit = true;
            lock.writeLock().unlock();
        }
    }

    /**
     * Get table information
     */
    public TableInfo getTableInfo(String tableName) {
        lock.readLock().lock();
        try {
            Table table = tables.get(tableName);
            if (table == null) {
                return null;
            }

            return new TableInfo(tableName, table.getColumns(), table.getRecordCount());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * List all tables
     */
    public List<String> listTables() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tables.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get database statistics
     */
    public DatabaseStats getStats() {
        lock.readLock().lock();
        try {
            int tableCount = tables.size();
            int totalRecords = 0;

            for (Table table : tables.values()) {
                totalRecords += table.getRecordCount();
            }

            return new DatabaseStats(tableCount, totalRecords);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void createDatabaseDirectory() {
        File dir = new File(dbPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void loadTables() {
        File dbDir = new File(dbPath);
        File[] files = dbDir.listFiles((dir, name) -> name.endsWith(".table"));

        if (files != null) {
            for (File file : files) {
                String tableName = file.getName().replace(".table", "");
                loadTable(tableName);
            }
        }
    }

    private void loadTable(String tableName) {
        try {
            String tablePath = dbPath + File.separator + tableName + ".table";
            File tableFile = new File(tablePath);

            if (!tableFile.exists()) {
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tableFile))) {
                Table table = (Table) ois.readObject();
                tables.put(tableName, table);
            }
        } catch (Exception e) {
            System.err.println("Error loading table " + tableName + ": " + e.getMessage());
        }
    }

    private void saveTable(Table table) {
        try {
            String tablePath = dbPath + File.separator + table.getName() + ".table";
            File tableFile = new File(tablePath);

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tableFile))) {
                oos.writeObject(table);
            }
        } catch (Exception e) {
            System.err.println("Error saving table " + table.getName() + ": " + e.getMessage());
        }
    }

    private void deleteTableFile(String tableName) {
        String tablePath = dbPath + File.separator + tableName + ".table";
        File tableFile = new File(tablePath);
        tableFile.delete();
    }

    /**
     * Column definition class
     */
    public static class Column implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final DataType type;
        private final boolean nullable;
        private final boolean primaryKey;

        public Column(String name, DataType type, boolean nullable, boolean primaryKey) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getName() { return name; }
        public DataType getType() { return type; }
        public boolean isNullable() { return nullable; }
        public boolean isPrimaryKey() { return primaryKey; }
    }

    /**
     * Data types enum
     */
    public enum DataType {
        INTEGER, STRING, BLOB, TIMESTAMP
    }

    /**
     * Record class
     */
    public static class Record implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<String, Object> values;
        private final long id;

        public Record(Map<String, Object> values) {
            this.values = new HashMap<>(values);
            this.id = System.currentTimeMillis() + (long) (Math.random() * 1000);
        }

        public Object getValue(String columnName) {
            return values.get(columnName);
        }

        public void setValue(String columnName, Object value) {
            values.put(columnName, value);
        }

        public Map<String, Object> getValues() {
            return new HashMap<>(values);
        }

        public long getId() {
            return id;
        }
    }

    /**
     * Table class
     */
    public static class Table implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final List<Column> columns;
        private final List<Record> records;
        private final Map<String, Map<Object, List<Record>>> indexes;

        public Table(String name, List<Column> columns) {
            this.name = name;
            this.columns = new ArrayList<>(columns);
            this.records = new ArrayList<>();
            this.indexes = new HashMap<>();
        }

        public String getName() { return name; }
        public List<Column> getColumns() { return new ArrayList<>(columns); }
        public int getRecordCount() { return records.size(); }

        public boolean insert(Record record) {
            // Validate record against columns
            for (Column column : columns) {
                if (!column.isNullable() && record.getValue(column.getName()) == null) {
                    return false;
                }
            }

            records.add(record);
            updateIndexes(record);
            return true;
        }

        public List<Record> select(String whereClause) {
            if (whereClause == null || whereClause.trim().isEmpty()) {
                return new ArrayList<>(records);
            }

            List<Record> results = new ArrayList<>();
            for (Record record : records) {
                if (evaluateWhereClause(record, whereClause)) {
                    results.add(record);
                }
            }

            return results;
        }

        public int update(Map<String, Object> values, String whereClause) {
            int updated = 0;
            for (Record record : records) {
                if (whereClause == null || evaluateWhereClause(record, whereClause)) {
                    for (Map.Entry<String, Object> entry : values.entrySet()) {
                        record.setValue(entry.getKey(), entry.getValue());
                    }
                    updated++;
                }
            }

            if (updated > 0) {
                rebuildIndexes();
            }

            return updated;
        }

        public int delete(String whereClause) {
            List<Record> toDelete = new ArrayList<>();

            for (Record record : records) {
                if (whereClause == null || evaluateWhereClause(record, whereClause)) {
                    toDelete.add(record);
                }
            }

            records.removeAll(toDelete);

            if (!toDelete.isEmpty()) {
                rebuildIndexes();
            }

            return toDelete.size();
        }

        public boolean createIndex(String columnName) {
            if (indexes.containsKey(columnName)) {
                return false; // Index already exists
            }

            Map<Object, List<Record>> index = new HashMap<>();
            for (Record record : records) {
                Object value = record.getValue(columnName);
                index.computeIfAbsent(value, k -> new ArrayList<>()).add(record);
            }

            indexes.put(columnName, index);
            return true;
        }

        private void updateIndexes(Record record) {
            for (Map.Entry<String, Map<Object, List<Record>>> entry : indexes.entrySet()) {
                String columnName = entry.getKey();
                Map<Object, List<Record>> index = entry.getValue();
                Object value = record.getValue(columnName);
                index.computeIfAbsent(value, k -> new ArrayList<>()).add(record);
            }
        }

        private void rebuildIndexes() {
            indexes.clear();
            for (Record record : records) {
                updateIndexes(record);
            }
        }

        private boolean evaluateWhereClause(Record record, String whereClause) {
            // Simple WHERE clause evaluation
            // Format: "column = value" or "column != value"
            String[] parts = whereClause.trim().split("\\s+");
            if (parts.length != 3) {
                return false;
            }

            String columnName = parts[0];
            String operator = parts[1];
            String value = parts[2];

            Object recordValue = record.getValue(columnName);
            if (recordValue == null) {
                return false;
            }

            String recordValueStr = recordValue.toString();

            switch (operator) {
                case "=":
                    return recordValueStr.equals(value);
                case "!=":
                    return !recordValueStr.equals(value);
                default:
                    return false;
            }
        }
    }

    /**
     * Table information class
     */
    public static class TableInfo {
        private final String name;
        private final List<Column> columns;
        private final int recordCount;

        public TableInfo(String name, List<Column> columns, int recordCount) {
            this.name = name;
            this.columns = new ArrayList<>(columns);
            this.recordCount = recordCount;
        }

        public String getName() { return name; }
        public List<Column> getColumns() { return new ArrayList<>(columns); }
        public int getRecordCount() { return recordCount; }
    }

    /**
     * db.Database statistics class
     */
    public static class DatabaseStats {
        private final int tableCount;
        private final int totalRecords;

        public DatabaseStats(int tableCount, int totalRecords) {
            this.tableCount = tableCount;
            this.totalRecords = totalRecords;
        }

        public int getTableCount() { return tableCount; }
        public int getTotalRecords() { return totalRecords; }

        @Override
        public String toString() {
            return String.format("DatabaseStats{tableCount=%d, totalRecords=%d}", tableCount, totalRecords);
        }
    }
}


