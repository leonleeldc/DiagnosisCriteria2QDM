package edu.mayo.bmi.medtagger.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
public class GenCM {

	//provided that two hashmaps are provided, a confusion matrix and metrics can be built as follows
	public static void buildConfusionM(List<Integer> classList,HashMap<Integer, Integer> labelCorrectM,HashMap<Pair<Integer, Integer>, Integer> labelWrongM,String predType,PrintWriter pwResult){
		pwResult.print("class type& \t");
		for(int i=0;i<classList.size();i++){
			pwResult.print(classList.get(i)+" & \t");
		}
		pwResult.print("ithTotal \\\\ \n");
		pwResult.println("\\hline");
		int[] jjthTotal = new int [classList.size()];
		int[] iithTotal = new int [classList.size()];
		for(int ii=0;ii<classList.size();ii++){
			pwResult.print(" \t"+classList.get(ii)+"& \t");
			Object ithCType = classList.get(ii);
			for(int jj=0;jj<classList.size();jj++){
				Object jthCType = classList.get(jj);
				if(ithCType.equals(jthCType)){
					int iiCorrect = 0;
					if(labelCorrectM.containsKey(ithCType)){
						iiCorrect = labelCorrectM.get(ithCType); 
						iithTotal[ii] += iiCorrect;
						jjthTotal[jj]+=iiCorrect;
						pwResult.print(iiCorrect+" & \t");
					}else{
						pwResult.print(iiCorrect+" & \t");
					}

				}else{
					Pair ii2jjPair = new Pair(ithCType,jthCType);
					if(labelWrongM.containsKey(ii2jjPair)){
						int ii2jj = labelWrongM.get(ii2jjPair);
						iithTotal[ii] += ii2jj;
						jjthTotal[jj]+=ii2jj;
						pwResult.print(ii2jj+"& \t");;
					}else{
						int ii2jj=0;
						pwResult.print(ii2jj+" & \t");;
					}

				}
				//pwResult.println();
			}
			pwResult.println(iithTotal[ii]+"\\\\ \n");
			pwResult.println("\\hline");
		}

		int total  = 0;
		pwResult.print(" \tjjTotal & \t");
		for(int i=0;i<classList.size();i++){
			total+=jjthTotal[i];
			pwResult.print(jjthTotal[i]+"& \t");
		}
		pwResult.print(total+"\\\\ \n");
		pwResult.println("\\hline");

		pwResult.println("\\end{tabular}");
		pwResult.println("\\label{table:rd metrics}");
		pwResult.println("\\end{table}");
		
		pwResult.println();
		pwResult.println("\\begin{table}[ht]");
		pwResult.println("\\caption{Metrics for " +predType+ "}");
		pwResult.println("\\centering");
		pwResult.println("\\begin{tabular}{|c|c|c|c|}");
		pwResult.println("\\hline \\hline");
		//don't confuse precision and recall. precision=tp/(tp+fp), recall=tp/(tp+fn), true negative rate=tn/tn+fp, accuracy=(tn+tn)/(tp+tn+fp+fn)
		pwResult.println("\t\t & Precision &  \t\t Recall  & \t\t  F-score \\\\");
		pwResult.println("\\hline");
		for(int ii=0;ii<classList.size();ii++){

			Object iithClass = classList.get(ii);
			int correct = 0;
			if(labelCorrectM.containsKey(iithClass)){
				correct = labelCorrectM.get(iithClass);
			}
			
			double recall =(double) correct/iithTotal[ii];
			double precision = (double)correct/jjthTotal[ii];
			double F_score = (double)(2*recall*precision)/(recall+precision);
			pwResult.println(iithClass+" & \t"+precision+" & \t"+recall+"& \t"+F_score+ "\\\\");
			pwResult.println("\\hline");
		}
		pwResult.println("\\end{tabular}");
		pwResult.println("\\label{table:rd metrics}");
		pwResult.println("\\end{table}");
	}
}
