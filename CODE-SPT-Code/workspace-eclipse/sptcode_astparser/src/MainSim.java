import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

public class MainSim {

    private static final LevenshteinDistance lv = new LevenshteinDistance();
    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");

    // Hardcoded file paths
    private static final String FILE_PATH1 = "input/finetune_methods_valid_final.txt";
    private static final String FILE_PATH2 = "input/input1.txt";
    private static final String OUTPUT_FILE = "input/finetune_valid_compare_output.txt";

    public static void main(String[] args) {
        try {
            PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
            System.setOut(fileOut);  // Redirects standard output to "output.txt"
            long startTime = System.currentTimeMillis();  // Start time
            compareFiles(FILE_PATH1, FILE_PATH2);
            long endTime = System.currentTimeMillis();  // End time
            System.out.println("Start Time: " + startTime);
            System.out.println("End Time: " + endTime);
            System.out.println("Duration: " + (endTime - startTime) + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void compareFiles(String filePath1, String filePath2) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {

            // Load all lines from file1 into a list
            List<String> file1Lines = new ArrayList<>();
            String line1;
            while ((line1 = reader1.readLine()) != null) {
                file1Lines.add(processContent(line1, true));
            }

            String line2;
            int lineNumber = 1;

            // Compare each line from file2 against all lines in file1
            while ((line2 = reader2.readLine()) != null) {
                String content2 = processContent(replaceStrings(formatCode(line2)), false);
                System.out.printf("Comparing line %d from file2:\n", lineNumber);
                for (String content1 : file1Lines) {
                    double ratio = levenshteinRatio(content1, content2);
                    System.out.println("File1: " + content1);
                    System.out.println("File2: " + content2);
                    System.out.printf("Similarity Ratio: %.2f\n", ratio);
                }
                lineNumber++;
            }
        }
    }

    public static double levenshteinRatio(String s, String s1) {
        return 1 - ((double) lv.apply(s, s1)) / Math.max(s.length(), s1.length());
    }

    private static String processContent(String text, boolean removeAccessModifiers) {
        text = text.toLowerCase();
        if (removeAccessModifiers) {
            text = removeAccessModifiers(text);
        }
        text = text.replaceAll("\\s+", "");
        return text;
    }

    private static String replaceStrings(String text) {
        Matcher matcher = STRING_PATTERN.matcher(text);
        return matcher.replaceAll("___STR");
    }

    private static String removeAccessModifiers(String text) {
        return text.replaceAll("\\b(public|private|protected)\\b", "").trim();
    }

    private static String formatCode(String rawCode) {
        String formattedCode = rawCode.replace(" _ ", "")
                                     .replace(";", ";\n")
                                     .replace("{", "{\n")
                                     .replace("}", "\n}");
        return formattedCode;
    }
}

//import org.apache.commons.text.similarity.LevenshteinDistance;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.io.*;
//
//public class MainSim {
//
//    private static final LevenshteinDistance lv = new LevenshteinDistance();
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"|'[^']*'");
//
//
//    // Hardcoded file paths
//    private static final String FILE_PATH1 = "input/finetune_methods_test_final.txt";
//    private static final String FILE_PATH2 = "input/data.TargetType.seq.test.source.txt";
//
//    private static final String OUTPUT_FILE = "input/finetune_test_compare_output.txt";
//
//    public static void main(String[] args) {
//        try {
//        	PrintStream fileOut = new PrintStream(new FileOutputStream(OUTPUT_FILE));
//            System.setOut(fileOut);  // Redirects standard output to "output.txt"
//            compareFiles(FILE_PATH1, FILE_PATH2);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void compareFiles(String filePath1, String filePath2) throws IOException {
//    	try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
//             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {
//            
//            String line1, line2;
//            int lineNumber = 1;
//
//            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
//            	String content1 = processContent(line1, true);
//                String content2 = processContent(replaceStrings(formatCode(line2)), false);
//                double ratio = levenshteinRatio(content1, content2);
//                System.out.println(content1);
//                System.out.println(content2);
//                System.out.printf("Line %d - Similarity Ratio: %.2f\n", lineNumber, ratio);
//                lineNumber++;
//            }
//        }
//    }
//
//    public static double levenshteinRatio(String s, String s1) {
//        return 1 - ((double) lv.apply(s, s1)) / Math.max(s.length(), s1.length());
//    }
//
//    private static String processContent(String text, boolean removeAccessModifiers) {
//    	text = text.toLowerCase();
////        text = replaceStrings(text);
//        if (removeAccessModifiers) {
//            text = removeAccessModifiers(text);
//        }
//        text = text.replaceAll("\\s+", "");
//        return text;
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//    
////
////    private static String removeFirstWord(String text) {
////        String[] words = text.split("\\W+", 2);
////        return words.length > 1 ? words[1] : "";
////    }
//    
//    private static String removeAccessModifiers(String text) {
//        // Replace "public" or "private" or "protected" with nothing
//        return text.replaceAll("\\b(public|private|protected)\\b", "").trim();
//    }
//
//
//    private static String formatCode(String rawCode) {
//        String formattedCode = rawCode.replace(" _ ", "")
//                                     .replace(";", ";\n")
//                                     .replace("{", "{\n")
//                                     .replace("}", "\n}");
//        return formattedCode;
//    }
//}

////
//import org.apache.commons.text.similarity.LevenshteinDistance;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class MainSim {
//
//    private static LevenshteinDistance lv = new LevenshteinDistance();
//    private static final Pattern STRING_PATTERN = Pattern.compile("\"(?:\\\\\"|[^\"])*\"|'(?:\\\\'|[^'])*'");
//
//    public static void main(String[] args) {
//        String s = "public File getFile(String dirsProp, String path) throws IOException {    String[] dirs = getTrimmedStrings(dirsProp);    int hashCode = path.hashCode();    for (int i = 0; i < dirs.length; i++) {        int index = (hashCode + i &  Integer.MAX_VALUE ) % dirs.length;        File file = new File(dirs[index], path);        File dir = file.getParentFile();        if (dir.exists() || dir.mkdirs()) {            return file;        }    }    throw new IOException(___STR + dirsProp);}\"";
//        String s1 = "file get _ file ( string dirs _ prop , string path ) throws io _ exception { string [ ] dirs = get _ trimmed _ strings ( dirs _ prop ) ; int hash _ code = path . hash _ code ( ) ; for ( int i = 0 ; i < dirs . length ; i ++ ) { int index = ( hash _ code + i & PRED ) % dirs.length ; file file = new file ( dirs [ index ] , path ) ; file dir = file . get _ parent _ file ( ) ; if ( dir . exists ( ) || dir . mkdirs ( ) ) { return file ; } } throw new io _ exception ( \" _ no _  valid local directories in property: \" + dirs _ prop ) ; }";
//        String formattedContent2 = formatCode(s1);
//
//        String content1 = processContent(s, true);
//        String content2 = processContent(formattedContent2, false);
//
//        System.out.println(content1);
//        System.out.println(content2);
//        System.out.println("Similarity Ratio: " + levenshteinRatio(content1, content2));
//    }
//
//    public static double levenshteinRatio(String s, String s1) {
//        return 1 - ((double) lv.apply(s, s1)) / Math.max(s.length(), s1.length());
//    }
//
//    private static String processContent(String text, boolean removeFirstWord) {
//        text = text.toLowerCase();
//        text = replaceStrings(text);
//        if (removeFirstWord) {
//            text = removeFirstWord(text);
//        }
//        text = text.replaceAll("\\s+", "");
//        return text;
//    }
//
//    private static String replaceStrings(String text) {
//        Matcher matcher = STRING_PATTERN.matcher(text);
//        return matcher.replaceAll("___STR");
//    }
//
//    private static String removeFirstWord(String text) {
//        String[] words = text.split("\\W+", 2);
//        return words.length > 1 ? words[1] : "";
//    }
//
//    private static String formatCode(String rawCode) {
//        String formattedCode = rawCode.replace(" _ ", "")
//                                     .replace(";", ";\n")
//                                     .replace("{", "{\n")
//                                     .replace("}", "\n}");
//        return formattedCode;
//    }
//
//}
//
