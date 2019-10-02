package com.odi.beranet.beraodi.odiLib.dataBaseLibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.odi.beranet.beraodi.models.dataBaseItemModel;
import com.odi.beranet.beraodi.models.dataBaseProjectModel;
import kotlin.Unit;

import java.util.ArrayList;
import java.util.HashMap;

public class dbManager extends SQLiteOpenHelper {

    interface dbOnCallback {
        Unit insertCallback(boolean status);
    }

    interface onDataBase_allDataCallback {
        Unit allDataCallback(ArrayList<HashMap<String, String>> data);
    }

    interface onDataBase_ProjectInsertCallback {
        Unit callback(Boolean status);
    }

    interface onDataBase_ProjectDeleteCallback {
        Unit callback(Boolean status);
    }

    interface onDataBase_ProjectVideosCallBack {
        Unit allProjectVideosCallback(ArrayList<dataBaseItemModel> data);
    }

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "odiGallery";
    private static final String TABLE_NAME = "gallery";
    public static String ID = "id";
    public static String PATH = "path";
    public static String PROJECTID = "projectId";
    public static String CREATEDATE = "createDate";
    public static String THUMB = "thumb";
    public static String STATUS = "status";


    private static final String TABLE_NAME_2 = "gallery2";

    public dbManager(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PATH + " TEXT,"
                + THUMB + " TEXT,"
                + PROJECTID + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);
        System.out.println("Database => DB OLUŞTURULDU.");

        // tablo 2 şimdiye kadar eklenmiş projeler
        String CREATE_TABLE2 = "CREATE TABLE " + TABLE_NAME_2 + "("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + STATUS + " TEXT,"
                + CREATEDATE + " TEXT,"
                + PROJECTID + " TEXT" + ")";
        db.execSQL(CREATE_TABLE2);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }


    /**
     * item siler
     * @param id
     */
    public void deleteItem(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, ID + "=?", new String[]{ String.valueOf(id) });
    }

    public void deleteProjectItem(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME_2, ID + "=?", new String[]{ String.valueOf(id) });
    }

    /**
     * DB ye item ekler.
     * @param item = Bilgileri içeren data model
     */
    public void insertItem(dataBaseItemModel item, dbOnCallback callback){
        System.out.println("Database => item path: "+item.getVideoPath()+" projectId: "+item.getProjectId());
        SQLiteDatabase dbIN = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PATH, item.getVideoPath());
        values.put(PROJECTID, item.getProjectId());
        values.put(THUMB, item.getThumb());
        dbIN.insert(TABLE_NAME, null, values);
        dbIN.close();
        callback.insertCallback(true);
    }

    /**
     * Eklenmiş bütün satırları döndürür.
     * @return
     */
    public void getAllItem(onDataBase_allDataCallback callback){
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> numberList = new ArrayList<HashMap<String, String>>();

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();

                for(int i=0; i<cursor.getColumnCount();i++) {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }

                numberList.add(map);
            } while (cursor.moveToNext());
        }
        db.close();
        //return numberList;

        callback.allDataCallback(numberList);
    }

    public void getAllItemProject(onDataBase_allDataCallback callback){
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_NAME_2;
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> numberList = new ArrayList<HashMap<String, String>>();

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();

                for(int i=0; i<cursor.getColumnCount();i++) {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }

                numberList.add(map);
            } while (cursor.moveToNext());
        }
        db.close();

        System.out.println("Database => Proje Sayısı:: " + numberList.size());

        callback.allDataCallback(numberList);
    }

    /**
     * Bütün itemların toplam count döner
     * @return
     */
    public int getRowCount() {
        String countQuery = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int rowCount = cursor.getCount();
        db.close();
        cursor.close();
        return rowCount;
    }

    // **************************************** project
    // ****************************************

    /**
     * proje ekler - eğer aynı proje var ise eklemez...
     * @param dataModel
     * @param callback
     */
    public void insertProject(dataBaseProjectModel dataModel, onDataBase_ProjectInsertCallback callback) {
        Boolean checkStatus = false;

        for (int i = 0; i<getAllProject().size(); i++) {
            HashMap<String, String> itemData = getAllProject().get(i);
            String projectId = itemData.get(PROJECTID);

            if (projectId.equals(dataModel.getProjectId())) {
                checkStatus = true;
            }
        }

        if (checkStatus) { // zaten var ekleme yapılmaz
            callback.callback(false);
        }else { // yok ekleme yapılcak
            SQLiteDatabase dbIN = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(PROJECTID, dataModel.getProjectId());
            values.put(STATUS, "true");
            values.put(CREATEDATE, dataModel.getCreateDate());
            dbIN.insert(TABLE_NAME_2, null, values);
            dbIN.close();
            callback.callback(true);
        }
    }

    /**
     * Projeleri tutan db nin bütün itemlarını çeker
     * @return
     */
    public ArrayList<HashMap<String, String>>  getAllProject(){
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_NAME_2;
        Cursor cursor = db.rawQuery(selectQuery, null);
        ArrayList<HashMap<String, String>> numberList = new ArrayList<HashMap<String, String>>();
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                for(int i=0; i<cursor.getColumnCount();i++) {
                    map.put(cursor.getColumnName(i), cursor.getString(i));
                }
                numberList.add(map);
            } while (cursor.moveToNext());
        }
        db.close();
        return numberList;
    }


    /**
     * Projeyi ve projeye bağlı dosyaları siler. Ardından db deki satırı kaldırır.
     * @param projectId
     */
    public void deleteProject(final String projectId, final onDataBase_ProjectDeleteCallback callback) {
        getAllItemProject(new onDataBase_allDataCallback(){
            @Override
            public Unit allDataCallback(ArrayList<HashMap<String, String>> data) {
                Boolean status = false;
                for (int i=0; i < data.size(); i++) {
                    System.out.println("Database => Proje Karşılaştırması :: " + data.get(i).get(ID) + " = " + projectId);

                    if (data.get(i).get(ID).equals(projectId)) {
                        String id = data.get(i).get(ID);
                        status = true;
                        deleteProjectItem(id);
                    }
                }
                callback.callback(status);
                return null;
            }
        });
    }

    /**
     * Sadece projeyeId ile bağlantılı videoları geri döndürür.
     * @param projectId
     * @param callBack : ArrayList<dataBaseItemModel>
     */
    public void getProjectVideos(final String projectId, final onDataBase_ProjectVideosCallBack callBack){
        getAllItem(new onDataBase_allDataCallback(){
            ArrayList<dataBaseItemModel> itemModels = new ArrayList<dataBaseItemModel>();
            @Override
            public Unit allDataCallback(ArrayList<HashMap<String, String>> data) {
                for (int i=0; i < data.size(); i++) {
                    if (data.get(i).get(PROJECTID).equals(projectId)) {
                        String id = data.get(i).get(ID);
                        String projectId = data.get(i).get(PROJECTID);
                        String thumb = data.get(i).get(THUMB);
                        String path = data.get(i).get(PATH);
                        String createDate = data.get(i).get(CREATEDATE);
                        dataBaseItemModel item = new dataBaseItemModel(id,path,projectId,thumb);
                        itemModels.add(item);
                    }
                }
                callBack.allProjectVideosCallback(itemModels);
                return null;
            }
        });
    }
}
