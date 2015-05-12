/*
 * Copyright (c) 2013, David Brodsky. All rights reserved.
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kickflip.sdk;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * @hide
 */
public class FileUtils {

    static final String TAG = "FileUtils";

    static final String OUTPUT_DIR = "HWEncodingExperiments";       // Directory relative to External or Internal (fallback) Storage

    /**
     * Returns a Java File initialized to a directory of given name
     * at the root storage location, with preference to external storage.
     * If the directory did not exist, it will be created at the conclusion of this call.
     * If a file with conflicting name exists, this method returns null;
     *
     * @param c the context to determine the internal storage location, if external is unavailable
     * @param directory_name the name of the directory desired at the storage location
     * @return a File pointing to the storage directory, or null if a file with conflicting name
     * exists
     */
    public static File getRootStorageDirectory(Context c, String directory_name){
        File result;
        // First, try getting access to the sdcard partition
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            Log.d(TAG,"Using sdcard");
            result = new File(Environment.getExternalStorageDirectory(), directory_name);
        } else {
            // Else, use the internal storage directory for this application
            Log.d(TAG,"Using internal storage");
            result = new File(c.getApplicationContext().getFilesDir(), directory_name);
        }

        if(!result.exists())
            result.mkdir();
        else if(result.isFile()){
            return null;
        }
        Log.d("getRootStorageDirectory", result.getAbsolutePath());
        return result;
    }

    /**
     * Returns a Java File initialized to a directory of given name
     * within the given location.
     *
     * @param parent_directory a File representing the directory in which the new child will reside
     * @return a File pointing to the desired directory, or null if a file with conflicting name
     * exists or if getRootStorageDirectory was not called first
     */
    public static File getStorageDirectory(File parent_directory, String new_child_directory_name){

        File result = new File(parent_directory, new_child_directory_name);
        if(!result.exists())
            if(result.mkdir())
                return result;
            else{
                Log.e("getStorageDirectory", "Error creating " + result.getAbsolutePath());
                return null;
            }
        else if(result.isFile()){
            return null;
        }

        Log.d("getStorageDirectory", "directory ready: " + result.getAbsolutePath());
        return result;
    }

    /**
     * Returns a TempFile with given root, filename, and extension.
     * The resulting TempFile is safe for use with Android's MediaRecorder
     * @param c
     * @param root
     * @param filename
     * @param extension
     * @return
     */
    public static File createTempFile(Context c, File root, String filename, String extension){
        File output = null;
        try {
            if(filename != null){
                if(!extension.contains("."))
                    extension = "." + extension;
                output = new File(root, filename + extension);
                output.createNewFile();
                //output = File.createTempFile(filename, extension, root);
                Log.i(TAG, "Created temp file: " + output.getAbsolutePath());
            }
            return output;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File createTempFileInRootAppStorage(Context c, String filename){
        File recordingDir = FileUtils.getRootStorageDirectory(c, OUTPUT_DIR);
        return createTempFile(c, recordingDir, filename.split("\\.")[0], filename.split("\\.")[1]);
    }
    
    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();        
        return ret;
    }
    
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    public static void writeStringToFile(String source, File dest, boolean append){
		try {
			FileWriter writer = new FileWriter(dest, append);
			writer.write(source);
	    	writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * Read the last few lines of a file
     * @param file the source file
     * @param lines the number of lines to read
     * @return the String result
     */
    public static String tail2( File file, int lines) {
        lines++;    // Read # lines inclusive
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler =
                    new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                if( readByte == 0xA ) {
                    line = line + 1;
                    if (line == lines) {
                        if (filePointer == fileLength - 1) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            return null;
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            return null;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                /* ignore */
                }
        }
    }

    /**
     * Delete a directory and all its contents
     */
    public static void deleteDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteDirectory(child);

        fileOrDirectory.delete();
    }

}