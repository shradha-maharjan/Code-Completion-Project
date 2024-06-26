/**
 * @(#) UTFile.java
 */
package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @since J2SE-1.8
 */
public class UtilFile {

   public static List<String> readFile(String filePath) {
      List<String> contents = new ArrayList<String>();
      File file = new File(filePath);
      Scanner scanner = null;
      try {
         scanner = new Scanner(file);
         while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            contents.add(line);
         }
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } finally {
         if (scanner != null)
            scanner.close();
      }
      return contents;
   }

   public static String readEntireFile(String filename) throws IOException {
      FileReader in = new FileReader(filename);
      StringBuilder contents = new StringBuilder();
      char[] buffer = new char[4096];
      int read = 0;
      do {
         contents.append(buffer, 0, read);
         read = in.read(buffer);
      }
      while (read >= 0);
      in.close();
      return contents.toString();
   }

   public static String getShortFileName(String fileName) {
      String S = System.getProperty("file.separator");
      int idx = fileName.lastIndexOf(S);
      if (idx == -1) {
         idx = fileName.lastIndexOf("/");
         if (idx == -1) {
            idx = fileName.lastIndexOf("\\");
         }
      }
      return fileName.substring(idx + 1);
   }
}
