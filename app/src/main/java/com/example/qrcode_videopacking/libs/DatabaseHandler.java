package com.example.qrcode_videopacking.libs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {

	Context context;
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAMA = "antrian";
	public static final String TABLE_UPLOAD   = "upload";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAMA, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public void createTable() {
		SQLiteDatabase db = this.getWritableDatabase();
		
		//db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPLOAD);
		db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_UPLOAD + "(nama_file TEXT, status_hapus TEXT)");
		db.close();
	}

	public void inserUploadtData(String nama_file) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
        values.put("nama_file", nama_file);
        values.put("status_hapus", "N");
        db.insert(TABLE_UPLOAD, null, values);
		db.close();
	}

	public void deleteUploadData() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_UPLOAD, "status_hapus=?", new String[] { "Y" });
		db.close();
	}

    public void deleteUploadData(String source) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_UPLOAD, "nama_file=?", new String[] { source });
        db.close();
    }

    public void updateUploadData(String source) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status_hapus", "Y");
        db.update(TABLE_UPLOAD, values, "nama_file=?", new String[] { source });
        db.close();
    }

    public ArrayList<String> getUploadData() {

        ArrayList<String> result = new ArrayList<>();
        try {
            String sql = "SELECT * FROM " + TABLE_UPLOAD + " WHERE status_hapus='N' ORDER BY nama_file ASC LIMIT 0, 1";
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sql, null);
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                for(int i=0; i<cursor.getCount(); i++) {
                    result.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            cursor.close();
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
