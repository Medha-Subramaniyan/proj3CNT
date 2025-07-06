package project3.util;

import javax.swing.table.AbstractTableModel;
import java.sql.*;

/**
 * Wraps a JDBC ResultSet in a TableModel for use in a JTable.
 * Loads all data into memory so row/column methods don't throw checked exceptions.
 */
public class ResultSetTableModel extends AbstractTableModel {
    private final String[] columnNames;
    private final Object[][] data;

    /**
     * Reads all rows and columns from the ResultSet into arrays.
     * Any SQL error is turned into a RuntimeException.
     */
    public ResultSetTableModel(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // read column names
            columnNames = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                columnNames[i - 1] = meta.getColumnLabel(i);
            }

            // read all rows
            rs.last();
            int rowCount = rs.getRow();
            data = new Object[rowCount][colCount];
            rs.beforeFirst();
            int rowIndex = 0;
            while (rs.next()) {
                for (int colIndex = 1; colIndex <= colCount; colIndex++) {
                    data[rowIndex][colIndex - 1] = rs.getObject(colIndex);
                }
                rowIndex++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading ResultSet", e);
        }
    }

    @Override
    public int getRowCount() {
        return data.length;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex][columnIndex];
    }
}
