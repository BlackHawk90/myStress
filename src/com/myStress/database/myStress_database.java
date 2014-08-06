package com.myStress.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class myStress_database extends SQLiteOpenHelper 
{
	/**
	 * Name of myStress database
	 */
	public static final String DATABASE_NAME = "myStress";
    private static final int DATABASE_VERSION = 2;
	/**
	 * name of main DB table with recording values
	 */
    public static final String DATABASE_TABLE_NAME = "myStress_values";
	/**
	 * SQL command for creating the main 'myStress_values' table
	 */
    public static final String DATABASE_TABLE_CREATE =
                "CREATE TABLE IF NOT EXISTS " + DATABASE_TABLE_NAME + " ( Timestamp BIGINT, Symbol CHAR(2), Value TEXT);";
	/**
	 * SQL command for creating the 'myStress_dates' table, which holds the dates at which something was recorded
	 */
    public static final String DATABASE_TABLE_CREATE2 =
        "CREATE TABLE IF NOT EXISTS myStress_dates (Year INT, Month INT, Day INT, Types INT);";
	/**
	 * SQL command for creating the 'myStress_sensors_used' table, which holds the sensors being used at a particular date
	 */
    public static final String DATABASE_TABLE_CREATE3 =
        "CREATE TABLE IF NOT EXISTS myStress_sensors_used (Timestamp BIGINT, Symbol CHAR(2));";
	/**
	 * SQL command for creating the index 'myStress_sensors_used_timestamp'
	 */
    public static final String DATABASE_TABLE_INDEX3= 
    	"CREATE INDEX IF NOT EXISTS myStress_sensors_used_timestamp ON myStress_sensors_used (Timestamp)";
    
	/**
	 * Constructor for opening the database class
	 * @param context Android {@link android.content.Context} in which the database is opened
	 */
    public myStress_database(Context context) 
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

	/**
	 * Called when creating the myStress tables for the first time
	 * @param db SQliteDatabase variable being used
	 */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_TABLE_CREATE);
        db.execSQL(DATABASE_TABLE_CREATE2);
        db.execSQL(DATABASE_TABLE_CREATE3);
        db.execSQL(DATABASE_TABLE_INDEX3);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
    }
}
