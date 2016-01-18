package edu.mayo.bmi.medtagger.ml.util;

/**
 * this is a cool class. We can use it when we want to return more than one values. We can add as many
 * as possible. we can use it as return new TwoReturnValues(s,n);
 * copied from the following link.
 * http://www.osnews.com/story/20076/Multiple_Return_Values_in_Java
 * @author m048100
 */

public class MultipleReturns{
	public class TwoReturnValues {

		private Object first;
		private Object second;

		public TwoReturnValues(Object first, Object second) {
			this.first = first;
			this.second = second;
		}

		public Object getFirst() {
			return first;
		}

		public Object getSecond() {
			return second;
		}
	}

	/**
	 * A perfect object-oriented approach for having a wrapper around two objects. And since Java 1.5 supports autoboxing we don't have to give primitive types a special treatment, right? Well, this will really result in writing ugly code. Lets say some arbitrary methods needs to return a String s and an int n. No problem just write: return new TwoReturnValues(s,n);
	 * For the callee it gets more cumbersome. Both getFirst and getSecond return java.lang.Object. Though the callee knows about the real types, he has to cast first to String and second to int. This is ugly and it is common knowledge that casts spread around your code should be avoided. Lets make things easier and add generics:
	 */

	public class GenericTwoReturnValues<R,S> {

		private R first;
		private S second;

		public GenericTwoReturnValues(R first, S second) {
			this.first = first;
			this.second = second;
		}

		public R getFirst() {
			return first;
		}

		public S getSecond() {
			return second;
		}
	}




	/**
	 *Changing the signature of the method to TwoReturnValues >String,Integer< we not only got rid of those casts, but the code looks much more straightforward. One could argue that Generics itself are bloated fixings, but like I said if it is already in the language go ahead and use it. Wether or not the two accessore methods are needed in this specific case is open to dispute. Since this Object has no other purpose as to deliver values we can ignore encapsulation at least a bid and make first and second public. However then you should declare both fields final as well and regard the whole Object as immutable! This gives you also the advantage of having a thread-safe object (unless the fields itself are mutable).
	 * @param <R>
	 * @param <S>
	 */
	public final class ThreadSafeTwoReturnValues<R,S> {

		private R first;
		private S second;

		public ThreadSafeTwoReturnValues(R first, S second) {
			this.first = first;
			this.second = second;
		}

		public R getFirst() {
			return first;
		}

		public S getSecond() {
			return second;
		}
		
		public String toString(){
			return "the first: "+getFirst()+" the second: "+getSecond();
		}
	}
	
	/**
	 *Changing the signature of the method to TwoReturnValues >String,Integer< we not only got rid of those casts, but the code looks much more straightforward. One could argue that Generics itself are bloated fixings, but like I said if it is already in the language go ahead and use it. Wether or not the two accessore methods are needed in this specific case is open to dispute. Since this Object has no other purpose as to deliver values we can ignore encapsulation at least a bid and make first and second public. However then you should declare both fields final as well and regard the whole Object as immutable! This gives you also the advantage of having a thread-safe object (unless the fields itself are mutable).
	 * @param <R>
	 * @param <S>
	 */
	public final class ThreadSafeThreeReturnValues<P,R,S> {

		private P first;
		private R second;
		private S third;

		public ThreadSafeThreeReturnValues(P first, R second, S third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		public P getFirst() {
			return first;
		}

		public R getSecond() {
			return second;
		}
		
		public S getThird() {
			return third;
		}
		
		public String toString(){
			return "the first: "+getFirst()+" the second: "+getSecond()+"the third: "+getThird();
		}
	}
	
	/**
	 *Changing the signature of the method to TwoReturnValues >String,Integer< we not only got rid of those casts, but the code looks much more straightforward. One could argue that Generics itself are bloated fixings, but like I said if it is already in the language go ahead and use it. Wether or not the two accessore methods are needed in this specific case is open to dispute. Since this Object has no other purpose as to deliver values we can ignore encapsulation at least a bid and make first and second public. However then you should declare both fields final as well and regard the whole Object as immutable! This gives you also the advantage of having a thread-safe object (unless the fields itself are mutable).
	 * @param <R>
	 * @param <S>
	 */
	public final class ThreadSafeFourReturnValues<P,R,S,T> {

		private P first;
		private R second;
		private S third;
		private T fourth;

		public ThreadSafeFourReturnValues(P first, R second, S third, T fourth) {
			this.first = first;
			this.second = second;
			this.third = third;
			this.fourth = fourth;
		}

		public P getFirst() {
			return first;
		}

		public R getSecond() {
			return second;
		}
		
		public S getThird() {
			return third;
		}

		public T getFourth() {
			return fourth;
		}
		
		public String toString(){
			return "the first: "+getFirst()+" the second: "+getSecond()+"the third: "+getThird()+" the fourth: "+getFourth();
		}
	}
}