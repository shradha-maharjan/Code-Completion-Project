package prj_similarity;

import org.apache.commons.text.similarity.LevenshteinDistance;


public class MainSim {

	private static LevenshteinDistance lv = new LevenshteinDistance();

	public static void main(String[] args) {
		String s = "running";
		String s1 = "runninh";
		System.out.println(levensteinRatio(s, s1));
	}

	public static double levensteinRatio(String s, String s1) {
		return 1 - ((double) lv.apply(s, s1)) / Math.max(s.length(), s1.length());
	}

}