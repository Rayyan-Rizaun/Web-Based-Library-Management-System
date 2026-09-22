package com.lms.common.web;

/**
 * One tile for {@code components/stat-tile.html :: statTileRow(tiles)}.
 * {@code value} is a String, not a number — the caller formats it (with
 * thousands separators, a currency symbol, whatever the figure needs)
 * before handing it over; the fragment only renders text.
 *
 * <pre>
 *   List.of(
 *       new StatTile("Total Books", "1,248", false),
 *       new StatTile("Overdue", "14", true));
 * </pre>
 */
public record StatTile(String label, String value, boolean overdue) {

    public StatTile(String label, String value) {
        this(label, value, false);
    }
}
