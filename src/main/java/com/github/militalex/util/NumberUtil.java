package com.github.militalex.util;

/**
 * Service Class for Integers, Doubles, etc...
 *
 * @author Militalex
 * @version 1.0
 */
public class NumberUtil {
	/**
	 * Calculates the greatest common divisor with additive Euclidean algorithm.
	 * @return Returns the ggt
	 */
	public static int ggTAdd(int a, int b){
		//Überprüfung auf Null
		if (a == 0) return b;
		else if (b == 0) return a;
		
		while(b > 0){
			if (a > b) a -= b;
			else b -= a;
		}
		return a;
	}

	/**
	 * Calculates the greatest common divisor with multiplicative Euclidean algorithm.
	 * @return Returns the ggt
	 */
	public static int ggTMul(int a, int b){
		//Überprüfung auf Null
		if (a == 0) return b;
		if (b == 0) return a;
		
		while(b > 0){
			int z = a % b;
			a = b;
			b = z;
		}
		return a;
	}

	/**
	 * Shows while calculating the greatest common divisor with additive Euclidean algorithm, the computational steps.
	 * @return Returns the amount of steps needed for calculating
	 */
	public static int ggTAddOut(int a, int b){		
		int i;
		for(i = 0; b > 0; i++){
			System.out.println(a + " " + b);
			//Überprüfung auf Null
			if (i == 0){
				if (a == 0) return 0;
			}
			if (a > b) a -= b;
			else b -= a;
		}
		System.out.println(a + " " + b);
		return i;
	}
	/**
	 * Shows while calculating the greatest common divisor with multiplicative Euclidean algorithm, the computational steps.
	 * @return Returns the amount of steps needed for calculating
	 */
	public static int ggTMulOut(int a, int b){		
		int i;
		for(i = 0; b > 0; i++){		
			System.out.println(a +  " " + b);		
			//Überprüfung auf Null
			if (i == 0){
				if (a == 0) return 1;
			}
			int z = a % b;
			a = b;
			b = z;			
		}
		System.out.println(a +  " " + b);		
		return i;
	}

	/**
	 * Only calculates the greatest common divisor
	 * @return Returns gcd
	 */
	public static int ggT(int a, int b){
		return ggTMul(Math.abs(a), Math.abs(b));
	}

	/**
	 * Check if two doubles almost are equal
	 */
	public static boolean equals(double a, double b){
		double eps = 0.000001;
		return a == b || (a == 0 ? Math.abs(b) < eps : b == 0 ? Math.abs(a) < eps : Math.abs(a-b) / Math.min(Math.abs(a),Math.abs(b)) < eps);
	}

	/**
	 * Checks if a2 has the doubled elements of a1
	 */
	public static boolean haveDoubleValue(int[] a1, int[] a2){
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++){
			if (a1[i] != a2[i] * 2) return false;
		}
		return true;
	}

	/**
	 * @return Returns id @param str is a kind of number.
	 */
	public static boolean isNumber(String str){
		return isLong(str) || isDouble(str) || isInteger(str) || isFloat(str) || isShort(str) || isByte(str);
	}

	/**
	 * Checks if the @param str can convert to a Byte.
	 */
	public static boolean isByte(String str){
		try{
			Byte.parseByte(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}

	/**
	 * Checks if the @param str can convert to a Short.
	 */
	public static boolean isShort(String str){
		try{
			Short.parseShort(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}

	/**
	 * Checks if the @param str can convert to an Integer.
	 */
	public static boolean isInteger(String str){
		try{
			Integer.parseInt(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}

	/**
	 * Checks if the @param str can convert to an Long.
	 */
	public static boolean isLong(String str){
		try{
			Long.parseLong(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}

	/**
	 * Checks if the @param str can convert to a Float.
	 */
	public static boolean isFloat(String str){
		try{
			Float.parseFloat(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}

	/**
	 * Checks if the @param str can convert to a Double.
	 */
	public static boolean isDouble(String str){
		try{
			Double.parseDouble(str);
			return true;
		}
		catch (NumberFormatException ex){
			return false;
		}
	}
}