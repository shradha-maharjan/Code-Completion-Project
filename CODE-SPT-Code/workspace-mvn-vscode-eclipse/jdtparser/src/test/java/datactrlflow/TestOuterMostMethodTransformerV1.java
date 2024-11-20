package datactrlflow;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.junit.Test;

import datactrlflow.OuterMostMethodTransformer;
import util.UtilAST;
import visitor.DataFlowAnalyzer;

public class TestOuterMostMethodTransformerV1 {

   private static final String UNIT_NAME = "ParsedAndToString";

   String theLineOfFunction = "class ParsedAndToString {\n" //
         + "    public PackageResourceTable newFrameworkResourceTable(ResourcePath resourcePath) {\n" //
         + "      return PerfStatsCollector.getInstance().measure(\"load legacy framework resources\", () -> {\n" //
         + "        PackageResourceTable resourceTable = new PackageResourceTable(\"android\");\n" //

         /*
         + "        if (resourcePath.getRClass() != null) {\n" //
         + "          addRClassValues(resourceTable, resourcePath.getRClass());\n" //
         + "          addMissingStyleableAttributes(resourceTable, resourcePath.getRClass());\n" //
         + "        }\n" //
         
         + "        if (resourcePath.getInternalRClass() != null) {\n" //
         + "          addRClassValues(resourceTable, resourcePath.getInternalRClass());\n" //
         + "          addMissingStyleableAttributes(resourceTable, resourcePath.getInternalRClass());\n" //
         + "        }\n" //
         
         + "        parseResourceFiles(resourcePath, resourceTable);\n" //
         */
         + "        return resourceTable;\n" //
         + "      });\n" //
         + "    }\n" //
         + "  }";

   @Test
   public void testOffsetOfVarUsageModifiedCode() {
      // String theLineOfFunction = "class ParsedAndToString { public PackageResourceTable newFrameworkResourceTable( ResourcePath resourcePath){ return PerfStatsCollector.getInstance().measure(\"load legacy framework resources\",() -> { PackageResourceTable resourceTable=new PackageResourceTable(\"android\"); if (resourcePath.getRClass() != null) { addRClassValues(resourceTable,resourcePath.getRClass()); addMissingStyleableAttributes(resourceTable,resourcePath.getRClass()); } if (resourcePath.getInternalRClass() != null) { addRClassValues(resourceTable,resourcePath.getInternalRClass()); addMissingStyleableAttributes(resourceTable,resourcePath.getInternalRClass()); } parseResourceFiles(resourcePath,resourceTable); return resourceTable; } ); } }";
      // String theLineOfFunction = "class ParsedAndToString { public A m1( ResourcePath resourcePath){ return PerfStatsCollector.getInstance().measure(\"load legacy framework resources\",() -> { PackageResourceTable resourceTable=new PackageResourceTable(\"android\"); return resourceTable; })}}";

      // String theLineOfFunction = //
      // "public class AA { public static PP ff(String input, Supplier<PP> supplier) {\n" //
      // + " return supplier.get(); \n" //
      // + "}}" //
      // + "class AA{ } class ParsedAndToString {\n" //
      // + " public PP nn() {\n" //
      // + " BB bbbb = new BB(); return AA.ff(\"\", () -> {\n" //
      // + " PP rr = new PP(\"\");\n" //
      // + " return rr;\n" //
      // + " });\n" //
      // + " }\n" //
      // + " }";

      // theLineOfFunction = "class ParsedAndToString { public Set getSIBDestinationLocalitySet( String busName, String uuid, boolean newSet) throws SIBExceptionDestinationNotFound, SIBExceptionBase {__mymethod__4990(busName); return _localistySet; } }";
      // theLineOfFunction = "class ParsedAndToString { public FileEventOptions kind( final WatchEvent.Kind<Path> kind){" //
      theLineOfFunction = "class ParsedAndToString { public void kind(A.Kind<String> kind){ m1(kind); " //
            + "m1(kind); } };";
      // + "__mymethod__66(kind); requireNonNull(kind,\"WatchEvent.Kind required.\"); kinds.add(kind); return this; } };";

      System.out.println("[DBG] " + theLineOfFunction);
      ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      System.out.println("[DBG] " + cu);
      System.out.println("[DBG] ------------------------------------------------------");

      DataFlowAnalyzer dataFlowAnalyzer = new DataFlowAnalyzer(cu);
      cu.accept(dataFlowAnalyzer);

      dataFlowAnalyzer.getOffsetOfVarUsage();
      Map<IBinding, List<Integer>> offsetOfVarUsage = dataFlowAnalyzer.getOffsetOfVarUsage();

      // ** check if the def-use relationship.
      boolean checkDefUse = checkDefUseRelationship(offsetOfVarUsage);
      if (checkDefUse) {
         System.out.println("[DBG] `OKAY` - has more than 2 elements");
      }
   }

   boolean checkDefUseRelationship(Map<IBinding, List<Integer>> offsetOfVarUsage) {
      boolean hasMoreThanTwoValues = false;
      if (offsetOfVarUsage.size() == 0) {
         System.out.println("[DBG] `NOT OKAY` No map element has more than 2 values.");
         return false;
      }

      for (Map.Entry<IBinding, List<Integer>> entry : offsetOfVarUsage.entrySet()) {
         IBinding varBinding = entry.getKey();
         List<Integer> varUses = entry.getValue();
         if (varBinding != null && varUses != null && varUses.size() > 1) {
            hasMoreThanTwoValues = true;
            break;
         }
      }

      if (!hasMoreThanTwoValues) {
         System.out.println("[DBG] `NOT OKAY` No map element has more than 2 values.");
      }
      return hasMoreThanTwoValues;
   }

   // @Test
   public void testFindMethodDecl1() throws IOException {
      OuterMostMethodTransformer transformer = new OuterMostMethodTransformer();

      String theLine = null;

      /*      // Test case 1
      theLine = "public class ParsedAndToString {\n" //
            + "void m1() {\n" //
            + "    return new MyClass() {\n"//
            + "        void m2() {\n"//
            + "            return new MyList<String>().m4();\n" //
            + "        }\n" //
            + "    }.m3((new ArrayList < String > ()));\n" //
            + "}}";
      String modifyOuterMostMethod2 = transformer.modifyOuterMostMethod(0, theLine);
      System.out.println("Modified code for test case 1:\n" + modifyOuterMostMethod2);
      */
      // Test case 2
      theLine = "public class ParsedAndToString {\n" //
            + "void m1(String parm1, String parm2) {\n" //
            + "    return new MyClass() {\n"//
            + "        void m2() {\n"//
            + "            return new MyList<String>().m4();\n" //
            + "        }\n" //
            + "    }.m3((new ArrayList < String > ()));\n" //
            + "}}";
      String modifyOuterMostMethod3 = transformer.modifyOuterMostMethod(0, theLine);
      System.out.println("Modified code for test case 2:\n" + modifyOuterMostMethod3);
      /*   
      
      // Test case 3
      theLine = "class ParsedAndToString {\n" + "      private < R > Consumer3 < FusionTraceContext, Promise < S > , Settable < T >> compose(final Consumer3 < FusionTraceContext, Promise < S > , Settable < R >> predecessor, final Consumer3 < FusionTraceContext, Promise < R > , Settable < T >> propagator) {\n" + "\n" + "\n" + "         return (traceContext, src, dst) -> {\n" + "            traceContext.createSurrogate();predecessor.accept(traceContext, src, new Settable < R > () {\n" + "               @Override public void done(R value) throws PromiseResolvedException {\n" + "                  try {\n" + "                     getEffectiveShallowTraceBuilder(traceContext).setStartNanos(System.nanoTime());\n" + "                     propagator.accept(traceContext, Promises.value(value), dst);\n" + "                  } catch (Exception e) {\n" + "                     LOGGER.error(\"ParSeq ingternal error. An exception was thrown by propagator\", e);\n" + "                  }\n" + "               }\n" + "               @Override public void fail(Throwable error) throws PromiseResolvedException {\n"
            + "                  try {\n" + "                     getEffectiveShallowTraceBuilder(traceContext).setStartNanos(System.nanoTime());\n" + "                     propagator.accept(traceContext, Promises.error(error), dst);\n" + "                  } catch (Exception e) {\n" + "                     LOGGER.error(\"ParSeq ingternal error. An exception was thrown by propagator.\", e);\n" + "                  }\n" + "               }\n" + "            });\n" + "         };\n" + "      }\n" + "   }";
      String modifyOuterMostMethod4 = transformer.modifyOuterMostMethod(0, theLine);
      System.out.println("Modified code for test case 3:\n" + modifyOuterMostMethod4);
      
      // Test case 4
      theLine = "class ParsedAndToString {\n" + "    void outerMethod() {\n" + "        System.out.println(\"This is the outer method.\");\n" + "        InnerClass inner = new InnerClass();\n" + "        inner.innerMethod();\n" + "    }\n" + "\n" + "    class InnerClass {\n" + "        void innerMethod() {\n" + "            System.out.println(\"This is the inner class method.\");\n" + "        }\n" + "    }\n" + "}\n" + "";
      String modifyOuterMostMethod = transformer.modifyOuterMostMethod(0, theLine);
      System.out.println("Modified code for test case 4:\n" + modifyOuterMostMethod);*/
   }
}

//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
