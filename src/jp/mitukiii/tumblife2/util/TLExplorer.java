package jp.mitukiii.tumblife2.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import jp.mitukiii.tumblife2.Main;
import jp.mitukiii.tumblife2.exeption.TLSDCardNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

public class TLExplorer
{
  public static final String         FILE_SCHEME     = "file:";
  
  public static final String         SD_CARD         = Environment.getExternalStorageDirectory().getPath() + "/";
  public static final String         APP_DIR         = SD_CARD + Main.APP_NAME + "/";
  
  public static final String         HTML_DIR        = APP_DIR + "html/";
  public static final String         CSS_DIR         = APP_DIR + "css/";
  public static final String         JS_DIR          = APP_DIR + "js/";
  public static final String         IMAGE_DIR       = APP_DIR + "img/";

  public static final String         HTML_EXTENSION  = "html";
  
  public static final String         IMAGE_GIF_EXTENSION = "gif";
  
  public static final CompressFormat IMAGE_PNG_FORMAT    = CompressFormat.PNG;
  public static final String         IMAGE_PNG_EXTENSION = IMAGE_PNG_FORMAT.toString().toLowerCase();
  public static final int            IMAGE_PNG_QUALITY   = 100;
  
  public static final int            IO_BUFFER_SIZE  = 4 * 1024;
  
  public static String makeFile(String fileDir, String fileName, String fileString, boolean force)
    throws TLSDCardNotFoundException, FileNotFoundException, IOException
  {
    if (!isSDCardWriteble() ||
        !makeDirectory(new File(APP_DIR)) ||
        !makeDirectory(new File(fileDir)))
    {
      throw new TLSDCardNotFoundException("SDCard not found.");
    }
    String filePath = fileDir + fileName;
    String fileUrl = FILE_SCHEME + filePath;
    if (force) {
      TLLog.d("TLExplorer / makeFile : fileDir " + fileDir + " : fileName / " + fileName);
    } else if (new File(filePath).exists()) {
      TLLog.d("TLExplorer / makeFile : file exits. : fileDir " + fileDir + " : fileName / " + fileName);
      return fileUrl;
    }
    FileOutputStream fileOutput = null;
    try {
      fileOutput = new FileOutputStream(filePath);
      fileOutput.write(fileString.getBytes());
      fileOutput.flush();
    } finally {
      if (fileOutput != null) {
        try {
          fileOutput.close();
        } catch (IOException e) {
          TLLog.i("TLExplorer / makeFile :", e);
        }
      }
    }
    return fileUrl;
  }
  
  public static String makeFile(String fileDir, String fileName, InputStream fileInput, boolean force)
    throws TLSDCardNotFoundException, FileNotFoundException, IOException
  {
    byte[] bytes = new byte[fileInput.available()];
    fileInput.read(bytes);
    String fileString = new String(bytes); 
    return makeFile(fileDir, fileName, fileString, force);
  }
  
  public static String makeHtmlFile(String fileName, String fileString, boolean force)
    throws TLSDCardNotFoundException, FileNotFoundException, IOException
  {
    return makeFile(HTML_DIR, fileName, fileString, force);
  }
  
  public static String makeGifImageFile(String urlString, String fileName)
    throws TLSDCardNotFoundException, MalformedURLException, FileNotFoundException, IOException
  {
    return makeFile(IMAGE_DIR, fileName, TLConnection.get(urlString).getInputStream(), false);
  }
  
  public static String makePngImageFile(String urlString, String fileName)
    throws TLSDCardNotFoundException, MalformedURLException, FileNotFoundException, IOException
  {
    if (!isSDCardWriteble() ||
        !makeDirectory(new File(APP_DIR)) ||
        !makeDirectory(new File(IMAGE_DIR)))
    {
      throw new TLSDCardNotFoundException("SDCard not found.");
    }
    String filePath = IMAGE_DIR + fileName;
    String fileUrl = FILE_SCHEME + filePath;
    if (new File(filePath).exists()) {
      TLLog.d("TLExplorer / makeImageFile : file exits. : fileName / " + fileName);
      return fileUrl;
    } else {
      TLLog.d("TLExplorer / makeImageFile : fileName / " + fileName);
    }
    HttpURLConnection con = null;
    InputStream input = null;
    FileOutputStream fileOutput = null;
    BufferedOutputStream output = null;
    try {
      con = TLConnection.get(urlString);
      input = new BufferedInputStream(con.getInputStream(), IO_BUFFER_SIZE);
      ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
      output = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
      byte[] b = new byte[IO_BUFFER_SIZE];
      int read;
      while ((read = input.read(b)) != -1) {
        output.write(b, 0, read);
      }
      output.flush();
      byte[] data = dataStream.toByteArray();
      Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
      if (image == null) {
        throw new FileNotFoundException("Image writing failed.");
      }
      fileOutput = new FileOutputStream(filePath, false);
      image.compress(IMAGE_PNG_FORMAT, IMAGE_PNG_QUALITY, fileOutput);
    } catch (OutOfMemoryError e) {
      TLLog.e("TLExplorer / makeImageFile : fileName / " + fileName, e);
      throw new IOException(e.getMessage());
    } finally {
      try {
        if (output != null) {
          output.close();
        }
      } catch (IOException e) {
        TLLog.i("TLExplorer / makeImageFile :", e);
      }
      try {
        if (fileOutput != null) {
          fileOutput.close();
        }
      } catch (IOException e) {
        TLLog.i("TLExplorer / makeImageFile :", e);
      }
      try {
        if (input != null) {
          input.close();
        }
      } catch (IOException e) {
        TLLog.i("TLExplorer / makeImageFile :", e);
      }
      if (con != null) {
        con.disconnect();
      }
    }
    return fileUrl;
  }
  
  public static String getPreffix(String fileName) {
    fileName = new File(fileName).getName();
    int point = fileName.lastIndexOf(".");
    if (point != -1) {
      fileName = fileName.substring(0, point);
    } 
    return fileName;
  }
  
  public static boolean isSDCardWriteble()
  {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }
  
  protected static boolean makeDirectory(File dir)
  {
    if (dir.exists()) {
      return dir.isDirectory();
    } else {
      return dir.mkdir();
    }
  }
  
  protected static void deleteFiles(File file)
  {
    TLLog.i("TLExplorer / deleteFiles : fileName /" + file.getPath());
    
    if (!file.exists()) {
      return;
    }
    if (file.isFile()) {
      file.delete();
    }
    if (file.isDirectory()) {
      for (File f: file.listFiles()) {
        deleteFiles(f);
      }
    }
  }
}
