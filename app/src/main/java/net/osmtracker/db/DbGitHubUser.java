package net.osmtracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import net.osmtracker.GitHubUser;

public class DbGitHubUser extends DBGitHelper{

    Context context;

    public DbGitHubUser(@Nullable Context context) {
        super(context);
        this.context = context;
    }

    public long insertUser(String username, String token){
        long id = 0;
        try {
            DBGitHelper dbGitHelper = new DbGitHubUser(context);
            SQLiteDatabase db = dbGitHelper.getWritableDatabase();
            dbGitHelper.onCreate(db);

            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("token", token);

            id = db.insert(TABLE_USERS, null, values);
        }catch (Exception e){
            e.toString();
        }
        return id;
    }

    public GitHubUser getUser(){
        GitHubUser gitHubUserAUX = null;
        Cursor cursorGitHubUser = null;

        try {
            DBGitHelper dbHelper = new DBGitHelper(context);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            cursorGitHubUser = db.rawQuery("SELECT * FROM " + TABLE_USERS + " ORDER BY id DESC", null);

            if(cursorGitHubUser.moveToFirst()){
                gitHubUserAUX = new GitHubUser();
                gitHubUserAUX.setId(cursorGitHubUser.getInt(0));
                gitHubUserAUX.setUsername(cursorGitHubUser.getString(1));
                gitHubUserAUX.setToken(cursorGitHubUser.getString(2));
            }
            cursorGitHubUser.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return gitHubUserAUX;
    }
}
