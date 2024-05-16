/**
 * @(#) UTFile.java
 */
package util;

import java.io.FileReader;
import java.io.IOException;

/**
 * @since J2SE-1.8
 */
public class UtilFile {
   public static String readEntireFile(String filename) throws IOException {
      FileReader in = new FileReader(filename);
      StringBuilder contents = new StringBuilder();
      char[] buffer = new char[4096];
      int read = 0;
      do {
         contents.append(buffer, 0, read);
         read = in.read(buffer);
      } while (read >= 0);
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
