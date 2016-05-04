package com.harryven.appai;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Create data bases for holding all the app related data.
 */
public class SqlLiteInterface extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "AppData.db";

    public SqlLiteInterface(Context context) {
//            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        super(context, context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/"
                + DATABASE_NAME, null, DATABASE_VERSION);

        Log.d("SqlLiteInterface", "env path DB CREATED" + super.getWritableDatabase().getPath());
        try {
            new File(super.getWritableDatabase().getPath()+".txt").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_APP_CALLS_TABLE = "CREATE TABLE AppCalls_Table ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Number TEXT, "+
                "Name TEXT, "+
                "Duration INTEGER, "+ // DURATION IN SECONDS
                "Type INTEGER, "+      // TYPE 0,1,2,3,4,5,6 FOR DIFFERENT TYPES
                "DateofCall INTEGER )"; // DATE OF CALL IN SECONDS

        // create table to store the app calls
        db.execSQL(CREATE_APP_CALLS_TABLE);

        String CREATE_APP_SMS_TABLE = "CREATE TABLE AppSMS_Table ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Number NUMERIC, "+
                "Type INTEGER, "+      // TYPE 0,1,2,3,4,5,6 FOR DIFFERENT TYPES
                "DateofSMS INTEGER )"; // DATE OF SMS IN SECONDS

        // create table to store the app SMS
        db.execSQL(CREATE_APP_SMS_TABLE);

        String CREATE_APP_PACKAGE_TABLE = "CREATE TABLE AppPackage_Table ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Name TEXT, "+
                "FirstTimeStamp INTEGER, "+
                "LastTimeStamp INTEGER, "+
                "LastTimeUsed INTEGER, "+
                "ForegroundTime INTEGER )";

        // create table to store the app SMS
        db.execSQL(CREATE_APP_PACKAGE_TABLE);

        String CREATE_APP_BROWSER_TABLE = "CREATE TABLE AppBrowser_Table ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Title TEXT, "+
                "Url TEXT, "+
                "DateSeen INTEGER )";

        // create table to store the app SMS
        db.execSQL(CREATE_APP_BROWSER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older books table if existed
        db.execSQL("DROP TABLE IF EXISTS AppCalls_Table");
        db.execSQL("DROP TABLE IF EXISTS AppSMS_Table");
        db.execSQL("DROP TABLE IF EXISTS AppPackage_Table");
        db.execSQL("DROP TABLE IF EXISTS AppBrowser_Table");

        this.onCreate(db);
    }

}

