/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.be.irail;

import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_INDEX_ID;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_INDEX_NAME;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_CREATE_TABLE_STATIONS;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.SQL_DELETE_TABLE_STATIONS;
import static be.hyperrail.opentransportdata.be.irail.IrailStationsDataContract.StationsDataColumns;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.RawRes;

import java.io.InputStream;
import java.util.Scanner;

import be.hyperrail.opentransportdata.be.R;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;
import be.hyperrail.opentransportdata.util.StringUtils;

/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * (c) Bert Marcelis 2018
 */
class IrailStopsDatabase extends SQLiteOpenHelper {
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailStopsDatabase.class);
    private static IrailStopsDatabase instance;
    private final Resources mResources;

    IrailStopsDatabase(Context context) {
        super(context, "irail-stations.db", null, 2026012300);
        this.mResources = context.getResources();
    }

    public static IrailStopsDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new IrailStopsDatabase(context);
        }
        return instance;
    }

    @RawRes
    private int getEmbeddedDataResourceId() {
        return R.raw.stations;
    }

    @Override
    public String getDatabaseName() {
        return "irail-stations.db";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabaseStructure(db);
        loadLocalData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        deleteDatabase(db);
        createDatabaseStructure(db);
        loadLocalData(db);
    }

    private void createDatabaseStructure(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_STATIONS);
        db.execSQL(SQL_CREATE_INDEX_ID);
        db.execSQL(SQL_CREATE_INDEX_NAME);
    }

    private void loadLocalData(SQLiteDatabase db) {
        db.beginTransaction();
        try (Scanner lines = new Scanner(getLocalData())) {
            int rowCount = importData(db, lines);
            if (rowCount == 0) {
                throw new RuntimeException("No station data was imported from CSV file");
            }
            log.info("Successfully loaded " + rowCount + " stations into database");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            log.severe("Failed to fill stations db with local data!", e);
            throw new RuntimeException("Failed to initialize stations database", e);
        } finally {
            db.endTransaction();
        }
    }

    private void deleteDatabase(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_TABLE_STATIONS);
    }

    private InputStream getLocalData() {
        return mResources.openRawResource(getEmbeddedDataResourceId());
    }

    private int importData(SQLiteDatabase db, Scanner lines) {
        lines.useDelimiter("\n");
        int rowCount = 0;

        while (lines.hasNext()) {
            String line = lines.next().trim();
            if (line.isEmpty() || line.startsWith("URI,name")) {
                // Header line or empty line
                continue;
            }

            // Use String.split() instead of Scanner for proper CSV handling
            // -1 limit preserves empty strings between delimiters
            String[] fields = line.split(",", -1);
            if (fields.length >= 11) {  // Ensure we have all required fields
                importRow(db, fields);
                rowCount++;
            } else {
                log.warning("Skipping malformed CSV line with " + fields.length + " fields: " + line);
            }
        }
        return rowCount;
    }

    private void importRow(SQLiteDatabase db, String[] fields) {
        ContentValues values = new ContentValues();

        // Store ID as URI
        values.put(StationsDataColumns._ID, fields[0]);

        // Replace special characters (for search purposes)
        values.put(
                StationsDataColumns.COLUMN_NAME_NAME,
                StringUtils.cleanAccents(fields[1])
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_FR,
                StringUtils.cleanAccents(fields[2])
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_NL,
                StringUtils.cleanAccents(fields[3])
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_DE,
                StringUtils.cleanAccents(fields[4])
        );
        values.put(
                StationsDataColumns.COLUMN_NAME_ALTERNATIVE_EN,
                StringUtils.cleanAccents(fields[5])
        );
        values.put(StationsDataColumns.COLUMN_NAME_COUNTRY_CODE, fields[6]);

        insertDoubleSafely(values, fields[7], StationsDataColumns.COLUMN_NAME_LONGITUDE);
        insertDoubleSafely(values, fields[8], StationsDataColumns.COLUMN_NAME_LATITUDE);
        insertDoubleSafely(values, fields[9], StationsDataColumns.COLUMN_NAME_AVG_STOP_TIMES);
        insertDoubleSafely(values, fields[10], StationsDataColumns.COLUMN_NAME_OFFICIAL_TRANSFER_TIME);

        // Insert row
        db.insert(StationsDataColumns.TABLE_NAME, null, values);
    }

    private void insertDoubleSafely(ContentValues values, String field, String columnName) {
        if (field != null && !field.isEmpty()) {
            try {
                values.put(
                        columnName,
                        Double.parseDouble(field)
                );

            } catch (NumberFormatException e) {
                values.put(columnName, 0);
            }
        } else {
            values.put(columnName, 0);
        }
    }
}
