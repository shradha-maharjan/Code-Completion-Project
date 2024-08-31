//package pkg1;
public class ClassA { public File getFile(String dirsProp, String path) throws IOException {    String[] dirs = getTrimmedStrings(dirsProp);    int hashCode = path.hashCode();    for (int i = 0; i < dirs.length; i++) {                int index = (hashCode + i &  Integer.MAX_VALUE ) % dirs.length;        File file = new File(dirs[index], path);        File dir = file.getParentFile();        if (dir.exists() || dir.mkdirs()) {            return file;        }    }    throw new IOException("No valid local directories in property: " + dirsProp);}}
// public class ClassA {
//    public void foo(int position, String buffer) {
//       // int oA = 0, oB = 0;

//       // String data = buffer.substring(10);
//       // int index = position / 100;

//       // if (index < 0) {
//       //    index = -1 * index;
//       //    System.out.println(data.charAt(index));
//       // } else {
//       //    index = index + 1;
//       //    System.out.println(data.charAt(index));
//       // }
      
//       ObjectA oA = new ObjectA();
//       oA.m1();
//       oA.m2();

//       ObjectB oB = new ObjectB();
//       oB.m3();
//       oB.m4();

//    }
// }
