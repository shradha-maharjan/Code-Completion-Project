
// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
// import org.eclipse.jdt.core.dom.*;

// import util.UtilAST;

// public class DefUseSimpleNameMain_correctImp2 {

//     static CompilationUnit cuInput1, cuInput2;

//     // Define input/output file paths
//     //static final String DIR = "/home/user1-selab3/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser/";
//     static final String INPUT1_FILE_PATH = "input/targettyperaw.txt";
//     static final String INPUT2_FILE_PATH = "input/targettypesource.txt";
//     static final String OUTPUT_FILE_PATH = "output/test_output.txt";
//     static final String OUTPUT_FILE_PRED_NULL_PATH = "output/null_test_output.txt";
//     static int counterNullPred = 0;

//     public static void main(String[] args) throws IOException {
//         // Read both Input 1 and Input 2 line by line
//         try (BufferedReader reader1 = new BufferedReader(new FileReader(INPUT1_FILE_PATH));
//              BufferedReader reader2 = new BufferedReader(new FileReader(INPUT2_FILE_PATH));
//              FileWriter writer = new FileWriter(OUTPUT_FILE_PATH);
//              FileWriter writer4PredNull = new FileWriter(OUTPUT_FILE_PRED_NULL_PATH);
//              ) {
//             String lineInput1;
//             String lineInput2;

//             int lineNum = 1;
//             while ((lineInput1 = reader1.readLine()) != null && (lineInput2 = reader2.readLine()) != null) {
//                 // Step 1: Find the offset of '[PRED]' in the current line of Input 2
//                 int predOffset = lineInput2.indexOf("PRED");

//                 if (predOffset != -1) {
//                     String predOffsetMessage = "[DBG] PRED Offset Found in line " + lineNum + ": " + predOffset;
//                     System.out.println(predOffsetMessage);
//                     writer.write(predOffsetMessage + "\n");
//                 } else {
//                     String predNotFoundMessage = "[DBG] [PRED] not found in line " + lineNum;
//                     System.out.println(predNotFoundMessage);
//                     writer.write(predNotFoundMessage + "\n");
//                     lineNum++;
//                     continue;
//                 }

//                 // Step 2: Parse the corresponding line from Input 1 (Raw input)
//                 String wrappedInput1Code = formatCode(lineInput1);
//                 ASTParser parser1 = UtilAST.parseSrcCode(wrappedInput1Code, UNIT_NAME + ".java");
//                 cuInput1 = (CompilationUnit) parser1.createAST(null);

//                 // Step 3: Parse Input 2 (Source input with PRED)
//                 String wrappedInput2Code = formatCode(lineInput2);
//                 ASTParser parser2 = UtilAST.parseSrcCode(wrappedInput2Code, UNIT_NAME + ".java");
//                 cuInput2 = (CompilationUnit) parser2.createAST(null);

//                 // Step 4: Find for-loop boundaries in Input 2
//                 List<int[]> loopBounds = ForLoopMatcher.findForLoopIndices(lineInput2);
//                 System.out.print("[DBG] Loop boundaries found in line " + lineNum + ": ");
//                 for (int[] bounds : loopBounds) {
//                     System.out.print(Arrays.toString(bounds) + " ");
//                 }
//                 System.out.println();


//                 // Step 5: Use PredOffsetFinder to check for 'PRED' in Input 2
//                 PredOffsetFinder predFinder = new PredOffsetFinder(predOffset, writer);
//                 cuInput2.accept(predFinder);
//                 if (predFinder.getPredOffset() == -1) {
//                     writer4PredNull.write(lineInput2 + "\n");
//                 }

//                 // Step 6: Use AllASTVisitor with loop bounds to check all node types and match the PRED offset
//                 AllASTVisitor allASTVisitor = new AllASTVisitor(writer, predFinder.getPredOffset(), loopBounds);
//                 cuInput1.accept(allASTVisitor);

//                 String separator = "[DBG] ------------------------------------------------------";
//                 System.out.println(separator);
//                 writer.write(separator + "\n");

//                 lineNum++;
//             }
            
//             counterNullPred++;
//             System.out.println("[DBG] Total Number of Null PRED: " + counterNullPred);
//         }
//     }

//     private static final String UNIT_NAME = "DummyClass";

//     private static String formatCode(String codeLine) {
//         return "public class " + UNIT_NAME + " {\n" + codeLine + "\n}";
//     }
// }

// class PredOffsetFinder extends ASTVisitor {
//     private int targetOffset = -1;
//     private int predOffset;
//     private FileWriter writer;

//     public PredOffsetFinder(int predOffset, FileWriter writer) {
//         this.predOffset = predOffset;
//         this.writer = writer;
//     }

//     @Override
//     public boolean visit(SimpleName node) {
//         System.out.println("[DBG] Checking SimpleName node: " + node + " at position: " + node.getStartPosition());
//         if (node.toString().equals("PRED")) {
//             targetOffset = node.getStartPosition();
//             try {
//                 writer.write("[DBG] Found PRED node at offset: " + targetOffset + "\n");
//             } catch (IOException e) {
//                 e.printStackTrace();
//             }
//         }
//         return true;
//     }

//     public int getPredOffset() {
//         return targetOffset;
//     }
// }
