package com.artifex.mupdfdemo;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.Uri;
import android.content.Intent;
import android.content.Context;
import com.artifex.mupdfdemo.MuPDFActivity;
import android.os.Environment;
import android.util.Log;

public class MuPdfPlugin extends CordovaPlugin {
  private CallbackContext callbackContext;

  private static String FILE_PREFIX = "file://";
  private static String ASSET = "android_asset";

    protected static final String LOG_TAG = "MuPDFPlugin";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if ("openPdf".equals(action)) {
      this.callbackContext = callbackContext;
      String fileUrl = args.getString(0);
      final String title = args.getString(1);
      final JSONObject options = args.getJSONObject(2);
      final boolean annotationsEnabled = options.getBoolean("annotationsEnabled");
      final boolean isAnnotatedPdf = options.getBoolean("isAnnotatedPdf");
      final String headerColor = options.getString("headerColor");

    	InputStream input;

    	 try {

      if ( fileUrl.startsWith( FILE_PREFIX ) )
      {
         fileUrl = fileUrl.replace( FILE_PREFIX, "" );
      }

      if ( fileUrl.contains( ASSET ) )
      {
        fileUrl = fileUrl.replace( "/" + ASSET + "/", "" );

        String filePath = "";
        String filename = fileUrl.substring(fileUrl.lastIndexOf("/")+1, fileUrl.length());

        input = cordova.getActivity()
             .getApplicationContext()
             .getAssets()
             .open( fileUrl );

        // Don't copy the file if it already exists
        File fp = new File(this.cordova.getActivity().getFilesDir() + "/" + filename);
        if (!fp.exists()) {
            this.copy(input, filename);
        }

        // change uri to be to the new file in internal storage
        fileUrl = FILE_PREFIX + this.cordova.getActivity().getFilesDir() + "/" + filename;
      } else {

        fileUrl = fileUrl.startsWith("/") ? fileUrl : "/" + fileUrl;
        fileUrl = Environment.getExternalStorageDirectory().toString() + fileUrl;
      }

      Uri uri = Uri.parse(fileUrl);

      Intent intent = new Intent(cordova.getActivity(), MuPDFActivity.class);

      intent.setAction(Intent.ACTION_VIEW);
      intent.putExtra(MuPDFActivity.KEY_TITLE, title);
      intent.putExtra(MuPDFActivity.KEY_HEADER_COLOR, headerColor);
      intent.putExtra(MuPDFActivity.KEY_ANNOTATIONS_ENABLED, annotationsEnabled);
      intent.putExtra(MuPDFActivity.KEY_IS_ANNOTATED_PDF, isAnnotatedPdf);
      intent.setData(uri);

      cordova.startActivityForResult(this, intent, 0);

      } catch (IOException e) {

          e.printStackTrace();
          Log.e(LOG_TAG, "Error loading asset "+fileUrl+": "+ e.toString());

          //return e.toString();
          this.callbackContext.error(e.toString());

      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();

        //return e.toString();
        this.callbackContext.error(e.toString());
      }

      return true;
    }
    return false;  // Returning false results in a "MethodNotFound" error.
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    switch (requestCode) {
    case 0: //integer matching the integer suplied when starting the activity
         if(resultCode == android.app.Activity.RESULT_OK){
             //in case of success return the string to javascript
             String result = intent.getStringExtra(MuPDFActivity.KEY_SAVE_RESULTS);
             if(result != null && result.length() > 0) {
               try {
                 final JSONObject saveResults = new JSONObject(result);
                 this.callbackContext.success(saveResults);
               } catch (JSONException e) {
                    e.printStackTrace();
                }

             } else {
               this.callbackContext.success();
             }
         }
         else{
             //code launched in case of error
             String message = "";
             if(intent != null) {
                 message = intent.getStringExtra("result");
             }
             this.callbackContext.error(message);
         }
         break;
    default:
         break;
    }
}

private void copy(InputStream in, String fileTo) throws IOException {
        // get file to be copied from assets
        //InputStream in = this.cordova.getActivity().getAssets().open(fileFrom);
        // get file where copied too, in internal storage.
        // must be MODE_WORLD_READABLE or Android can't play it
        FileOutputStream out = this.cordova.getActivity().openFileOutput(fileTo, Context.MODE_WORLD_READABLE);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
