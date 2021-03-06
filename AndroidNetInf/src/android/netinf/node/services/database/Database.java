package android.netinf.node.services.database;


import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.netinf.common.Ndo;
import android.netinf.messages.Get;
import android.netinf.messages.GetResponse;
import android.netinf.messages.Publish;
import android.netinf.messages.PublishResponse;
import android.netinf.messages.Search;
import android.netinf.messages.SearchResponse;
import android.netinf.node.get.GetService;
import android.netinf.node.publish.PublishService;
import android.netinf.node.search.SearchService;
import android.util.Log;

public class Database extends SQLiteOpenHelper implements PublishService, GetService, SearchService {

    public static final String TAG = Database.class.getSimpleName();

    public static final String DATABASE_NAME = "NdoDatabase.db3";
    private static final int DATABASE_VERSION = 1;

    private static final String TEXT = "TEXT";
    private static final String INTEGER = "INTEGER";
    private static final String BLOB = "BLOB";
    private static final String NOT_NULL = "NOT NULL";
    private static final String PRIMARY_KEY = "PRIMARY KEY";
    private static final String TABLE_NDO = "ndos";
    private static final String COLUMN_HASH_ALG = "alg";
    private static final String COLUMN_HASH = "hash";
    private static final String COLUMN_NDO = "ndo";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public synchronized PublishResponse perform(Publish publish) {

        Log.i(TAG, "Database PUBLISH " + publish);
        Ndo ndo = publish.getNdo();
        // TODO Don't just overwrite
        if (contains(ndo)) {
            Log.d(TAG, "Deleted: "+delete(ndo));
        }
        insert(ndo);
        //if (!contains(ndo)) {
        //    insert(ndo);
        //    Log.i(TAG,"Inserted new NDO into database");
        //} else {
        //    // TODO merge with existing
        //    Log.i(TAG,"NDO already in database");
        //}
        Log.i(TAG, "PUBLISH to database succeeded");
        return new PublishResponse.Builder(publish).ok().build(); // TODO check if actually inserted

    }

    @Override
    public synchronized GetResponse perform(Get get) {
        Log.i(TAG, "Database GET " + get);
        byte[] blob = getBlob(get.getNdo());
        Ndo ndo = null;
        if (blob != null ) {
            ndo = (Ndo) SerializationUtils.deserialize(blob);
            return new GetResponse.Builder(get).ok(get).build();
        }
        return new GetResponse.Builder(get).failed().build();
    }

    @Override
    public GetResponse resolveLocators(Get get) {
        // Not needed
        return new GetResponse.Builder(get).failed().build();
    }

    @Override
    public synchronized SearchResponse perform(Search search) {
        Log.i(TAG, "Database SEARCH " + search);
        String[] columns = {COLUMN_NDO};
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NDO, columns, null, null, null, null, null);
        cursor.moveToFirst();

        Set<Ndo> results = new HashSet<Ndo>();
        while (!cursor.isAfterLast()) {
            byte[] blob = cursor.getBlob(cursor.getColumnIndex(COLUMN_NDO));
            Ndo ndo = (Ndo) SerializationUtils.deserialize(blob);
            if (ndo.matches(search.getTokens())) {
                results.add(ndo);
            }
            cursor.moveToNext();
        }
        cursor.close();
        Log.i(TAG, "SEARCH in database produced " + results.size() + " NDO(s)");

        return new SearchResponse.Builder(search).addResults(results).build();

    }

    private byte[] getBlob(Ndo ndo) {
        String[] columns = {COLUMN_NDO};
        String selection = COLUMN_HASH_ALG + "=? AND " + COLUMN_HASH + "=?";
        String[] selectionArgs = {ndo.getAlgorithm(), ndo.getHash()};
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NDO, columns, selection, selectionArgs, null, null, null);
        cursor.moveToFirst();
        byte[] blob = null;
        if (!cursor.isAfterLast()) {
            blob = cursor.getBlob(cursor.getColumnIndex(COLUMN_NDO));
        }
        cursor.close();
        return blob;
    }

    private boolean contains(Ndo ndo) {
        return getBlob(ndo) != null;
    }

    private void insert(Ndo ndo) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_HASH_ALG, ndo.getAlgorithm());
        values.put(COLUMN_HASH, ndo.getHash());
        values.put(COLUMN_NDO, SerializationUtils.serialize(ndo));

        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_NDO, null, values);
    }

    private int delete(Ndo ndo) {
        SQLiteDatabase db = getWritableDatabase();
        String whereClause = COLUMN_HASH_ALG + "=? AND " + COLUMN_HASH + "=?";
        String[] whereArgs = new String[] {ndo.getAlgorithm(), ndo.getHash()};
        int deleted = db.delete(TABLE_NDO, whereClause , whereArgs);
        return deleted;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "(Re)creating table(s)...");
        String createTable = "CREATE TABLE " + TABLE_NDO + " ("
                + COLUMN_HASH_ALG + " " + TEXT + " " + NOT_NULL + ", "
                + COLUMN_HASH + " " + TEXT + " " + NOT_NULL + ", "
                + COLUMN_NDO + " " + BLOB + " " + NOT_NULL + ", "
                + PRIMARY_KEY + "(" + COLUMN_HASH_ALG + ", " + COLUMN_HASH + "));";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        clearDatabase();
    }

    public void clearDatabase() {
        clearDatabase(getWritableDatabase());
    }

    private void clearDatabase(SQLiteDatabase db) {
        Log.i(TAG, "Dropping table(s)...");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NDO);
        onCreate(db);
    }

}
