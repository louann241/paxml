/**
 * This file is part of PaxmlCore.
 *
 * PaxmlCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PaxmlCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PaxmlCore.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.paxml.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.paxml.core.PaxmlRuntimeException;
import org.paxml.util.RangedIterator;

public abstract class AbstractTable implements ITable {
	private boolean readonly;
	private ITableRange range;
	private ITableTransformer readTransformer;
	private int currentRowIndex;
	@Override
	public Iterator<IRow> iterator() {
		return getRows();
	}

	@Override
	public ITableRange getRange() {
		return range;
	}

	protected void setRange(ITableRange range) {
		this.range = range;
	}

	@Override
	public IColumn getColumn(int index) {
		return getColumns().get(index);
	}

	@Override
	public boolean isInsertable() {
		return isReadonly();
	}

	@Override
	public boolean isUpdatable() {
		return isReadonly();
	}

	@Override
	public boolean isAppendable() {
		return isReadonly();
	}

	@Override
	public boolean isDeletable() {
		return isReadonly();
	}

	@Override
	public boolean isReadonly() {
		return readonly;
	}

	public ITableTransformer getReadTransformer() {
		return readTransformer;
	}

	public void setReadTransformer(ITableTransformer readTransformer) {
		this.readTransformer = readTransformer;
	}

	abstract protected String getResourceIdentifier();

	protected void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public void assertWritable() {
		if (readonly) {
			throw new PaxmlRuntimeException("Resource is readonly: " + getResourceIdentifier());
		}
	}
	
	abstract protected Iterator<IRow> getAllRows();

	@Override
	public Iterator<IRow> getRows() {
		ITableRange r = getRange();
		Iterator<IRow> it;
		if (r == null) {
			 it = getAllRows();
		} else {
			it = new RangedIterator<IRow>(r.getFirstRow(), r.getLastRow(), getAllRows());
		}
		return new AbstractIteratorDecorator(it){

			@Override
            public Object next() {
	            Object n = super.next();
	            currentRowIndex++;
	            return n;
            }

			@Override
            public void remove() {
	            throw new UnsupportedOperationException(); 
            }
			
		};
	}

	@Override
    public int getCurrentRowIndex() {
	    return currentRowIndex;
    }

	@Override
	public List<RowDiff> compare(List<IColumn> myColumns, ITable against, List<IColumn> theirColumns, ICellComparator comp) {
		if (myColumns == null) {
			myColumns = getColumns();
		}
		if (theirColumns == null) {
			theirColumns = against.getColumns();
		}
		if (comp == null) {
			comp = new DefaultCellComparator();
		}
		Iterator<IRow> it1 = getRows();
		Iterator<IRow> it2 = against.getRows();
		List<RowDiff> rows = new ArrayList<RowDiff>(0);
		int index = 0;
		final int overlappedCols = Math.min(myColumns.size(), theirColumns.size());
		while (it1.hasNext() && it2.hasNext()) {
			IRow row1 = it1.next();
			IRow row2 = it2.next();
			RowDiff row = null;
			List<CellDiff> diffs = row1.compare(myColumns, row2, theirColumns, comp);
			if (diffs != null && !diffs.isEmpty()) {
				row = new RowDiff();
				row.setRowNumber(index);
				row.setCells(diffs);
			}
			index++;
		}

		return null;
	}

	protected void setCellValues(IRow rowDest, int from, int to, IRow rowSrc, ITableTransformer tran) {

		Map<String, Object> map = getTransformedCellValues(rowSrc, tran);
		for (int j = from; j <= to; j++) {
			String col = getColumn(j).getName();
			Object val = map.get(col);
			rowDest.setCellValue(j, val);
		}
	}

	protected Map<String, Object> getTransformedCellValues(IRow rowSrc, ITableTransformer tran) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Iterator<ICell> it = rowSrc.getCells(); it.hasNext();) {
			ICell cell = it.next();
			String newCol = tran == null ? cell.getColumn().getName() : tran.mapColumn(cell.getColumn());
			Object newVal = tran == null ? cell.getValue() : tran.transformValue(cell);
			result.put(newCol, newVal);
		}
		return result;
	}

	@Override
    public IRow getRow(int index) {
		// this source does not support getting a particular table row.
	    return null;
    }

}
