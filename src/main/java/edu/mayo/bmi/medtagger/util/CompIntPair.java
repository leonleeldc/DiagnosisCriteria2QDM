package edu.mayo.bmi.medtagger.util;

public class CompIntPair {
	
	public static OverlapType spanCompare(Pair<Integer,Integer> pair1,Pair<Integer,Integer> pair2){
		//a good lesson to learn: since I changed Pair to generic type. Even if Pair has type Integer, 
		//we cannot directly use ==; instead, we must cast its type to int. (Note: cast to Integer would not work)
		//System.out.println("In CompIntPair: "+pair1.getFirst()+" "+pair1.getSecond()+" "+pair2.getFirst()+" "+pair2.getSecond()+" "
				//+((int)pair1.getFirst()==(int)pair2.getFirst())+" "+((int)pair1.getSecond()==(int)pair2.getSecond()));
		
		if( (int)pair1.getFirst() == (int)pair2.getFirst() && (int)pair1.getSecond() == (int)pair2.getSecond()) {	
			return OverlapType.exact;
		}
		
		/* a obj subsumes pair2 span  */
		else if( ((int)pair1.getFirst() <= (int)pair2.getFirst()  && (int)pair1.getSecond() >  (int)pair2.getSecond())||
				((int)pair1.getFirst() < (int)pair2.getFirst()  && (int)pair1.getSecond() >=  (int)pair2.getSecond())) {
			return OverlapType.subsumes;
		}
		
		/* a obj is a subset of pair2 span  */
		else if( ((int)pair1.getFirst() >= (int)pair2.getFirst()  && (int)pair1.getSecond() <  (int)pair2.getSecond()) ||
				((int)pair1.getFirst() > (int)pair2.getFirst()  && (int)pair1.getSecond() <=  (int)pair2.getSecond())) {
			return OverlapType.subset;
		}
		
		else if( ((int)pair1.getFirst() < (int)pair2.getFirst())  && ((int)pair1.getSecond() <  (int)pair2.getSecond())  && ((int)pair1.getSecond() >  (int)pair2.getFirst()) ) {
			return OverlapType.overlap;
		}
		
		
		else if( ((int)pair1.getFirst() > (int)pair2.getFirst()) && ( (int)pair1.getFirst() < (int)pair2.getSecond()) && ((int)pair1.getSecond() >  (int)pair2.getSecond())   ) {
			return OverlapType.overlap;
		}
		
		
		else if( ((int)pair1.getFirst() < (int)pair2.getFirst())  &&  ((int)pair1.getSecond() <=  (int)pair2.getFirst()) ) {
			return OverlapType.leftseparate;
		}
		
		else if( ((int)pair1.getFirst() > (int)pair2.getFirst()) && ((int) pair1.getFirst() >= (int)pair2.getSecond()) && ((int)pair1.getSecond() >  (int)pair2.getSecond())  ) {
			return OverlapType.rightseparate;
		}
		return OverlapType.none;
	}
}
