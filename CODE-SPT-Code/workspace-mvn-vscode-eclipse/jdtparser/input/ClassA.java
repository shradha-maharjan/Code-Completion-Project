package pkg1;

public class ClassA {
   public void foo(int position, String buffer) {
      int oA = 0, oB = 0;

      String data = buffer.substring(10);
      int index = position / 100;

      if (index < 0) {
         index = -1 * index;
         System.out.println(data.charAt(index));
      } else {
         index = index + 1;
         System.out.println(data.charAt(index));
      }
      
      // ObjectA oA = new ObjectA();
      // oA.m1();
      // oA.m2();

      // ObjectB oB = new ObjectB();
      // oB.m3();
      // oB.m4();

   }
}
