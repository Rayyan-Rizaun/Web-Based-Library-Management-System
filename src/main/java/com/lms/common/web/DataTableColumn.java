package com.lms.common.web;

/**
 * One column header for {@code components/data-table.html}.
 *
 * <p>Build the list a controller passes as the fragment's {@code columns}
 * parameter with the static factories below rather than the constructor
 * directly, so a typo can't produce an {@code align} value the fragment
 * doesn't recognise:
 * <pre>
 *   List.of(
 *       DataTableColumn.left("Book"),
 *       DataTableColumn.left("Member"),
 *       DataTableColumn.left("Status"),
 *       DataTableColumn.right("Due date", "dueAt"));
 * </pre>
 *
 * <p>{@code sortKey} is the query parameter value the column's header link
 * sends back (e.g. {@code ?sort=dueAt&dir=asc}) — {@code null} for a column
 * that cannot be sorted. The fragment only renders the link and the correct
 * {@code aria-sort} / chevron; the controller is the one that has to read
 * {@code sort} and {@code dir} and actually order the query.
 */
public record DataTableColumn(String label, String align, String sortKey) {

    public DataTableColumn {
        if (!"left".equals(align) && !"right".equals(align)) {
            throw new IllegalArgumentException(
                    "DataTableColumn.align must be \"left\" or \"right\", was \"" + align + "\". "
                            + "Use DataTableColumn.left(...) / .right(...) instead of the constructor.");
        }
    }

    /** A text column — design-system.md §5: "text left". Not sortable. */
    public static DataTableColumn left(String label) {
        return new DataTableColumn(label, "left", null);
    }

    /** A sortable text column. */
    public static DataTableColumn left(String label, String sortKey) {
        return new DataTableColumn(label, "left", sortKey);
    }

    /** A number or date column — design-system.md §5: "numbers / dates right". Not sortable. */
    public static DataTableColumn right(String label) {
        return new DataTableColumn(label, "right", null);
    }

    /** A sortable number or date column. */
    public static DataTableColumn right(String label, String sortKey) {
        return new DataTableColumn(label, "right", sortKey);
    }

    public boolean sortable() {
        return sortKey != null;
    }
}
