import org.mymedialite.datatype.IMatrix;
import org.mymedialite.datatype.Matrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by host on 2017/8/15.
 */
public class BigMatrix<T> implements IMatrix<T> {

    /** Data array: data is stored in columns. */
    public Object[][] data;

    /** Dimension 1, the number of rows */
    public int dim1;

    /** Dimension 2, the number of columns */
    public int dim2;

    /**
     * Initializes a new instance of the Matrix class
     * @param dim1 the number of rows
     * @param dim2 the number of columns
     */
    public BigMatrix(int dim1, int dim2) {
        this(dim1, dim2, null);
    }

    /**
     * Initializes a new instance of the Matrix class
     * @param dim1 the number of rows
     * @param dim2 the number of columns
     * @param d the default value for the elements
     */
    public BigMatrix(int dim1, int dim2, T d) {
        if (dim1 < 0)
            throw new IllegalArgumentException("dim1 must be at least 0");
        if (dim2 < 0)
            throw new IllegalArgumentException("dim2 must be at least 0");

        this.dim1 = dim1;
        this.dim2 = dim2;
        this.data = new Object[dim1][dim2];

        if(d != null)
            for (int i = 0; i < dim1; i++) {
                for (int j = 0; j < dim2; j++) {
                    data[i][j] = d;
                }
            }
    }

    @Override
    public IMatrix<T> createMatrix(int num_rows, int num_columns) {
        return new Matrix<T>(num_rows, num_columns, null);
    }

    @Override
    public IMatrix<T> transpose() {
        return null;
    }

    @Override
    public int numberOfRows() {
        return dim1;
    }

    @Override
    public int numberOfColumns() {
        return dim2;
    }

    @Override
    public T get(int i, int j) {
        return (T) data[i][j];
    }

    @Override
    public void set(int i, int j, T value) {
        data[i][j] = value;
    }

    @Override
    public boolean isSymmetric() {
        if (dim1 != dim2)
            return false;
        for (int i = 0; i < dim1; i++)
            for (int j = i + 1; j < dim2; j++)
                if (!get(i, j).equals(get(j, i)))
                    return false;
        return true;
    }

    /**
     * Returns a copy of the i-th row of the matrix
     * @param i the row ID
     * @return a List<T> containing the row data
     */
    public List<T> getRow(int i) {
        List<T> row = new ArrayList<T>(this.dim2);
        for (int x = 0; x < this.dim2; x++)
            row.add(get(i, x));
        return row;
    }

    /**
     * Returns a copy of the j-th column of the matrix
     * @param j the column ID
     * @return T[] containing the column data
     */
    public List<T> getColumn(int j) {
        List<T> column = new ArrayList<T>(this.dim1);
        for (int x = 0; x < this.dim1; x++)
            column.set(x, get(x, j));
        return column;
    }

    /**
     * Sets the values of the i-th row to the values in a given array
     * @param i the row ID
     * @param row A of length dim1
     */
    public void setRow(int i, List<T> row) {
        if (row.size() != this.dim2)
            throw new IllegalArgumentException("Array length " + row.size() + " must equal number of columns " + this.dim2);
        for (int j = 0; j < this.dim2; j++)
            set(i, j, row.get(j));
    }

    /**
     * Sets the values of the j-th column to the values in a given array
     * @param j the column ID
     * @param column A T[] of length dim2
     */
    public void setColumn(int j, List<T> column) {
        if (column.size() != this.dim1)
            throw new IllegalArgumentException("Array length " + column.size() + " must equal number of rows " + this.dim1);
        for (int i = 0; i < this.dim1; i++)
            set(i, j, column.get(i));
    }

    /**
     * Grows the matrix to the requested size, if necessary
     * The new entries are filled with zeros.
     * @param num_rows the minimum number of rows
     * @param num_cols the minimum number of columns
     */
    @Override
    public void grow(int num_rows, int num_cols) {

    }
}
