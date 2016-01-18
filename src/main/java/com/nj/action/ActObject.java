package com.nj.action;

//<act classCode="ACT" moodCode="EVN" isCriterionInd="true"><!-- Laboratory Test, Result pattern -->
//<templateId root="2.16.840.1.113883.3.560.1.12" />
//<id root="40280381-3d61-56a7-013e-6224e2ce2648" />
//<code code="30954-2" displayName="Results" codeSystem="2.16.840.1.113883.6.1" />
//<sourceOf typeCode="COMP">
//	<observation classCode="OBS" moodCode="EVN"
//		isCriterionInd="true">
//		<code code="2.16.840.1.113883.3.464.1003.198.12.1033"
//			displayName="Hepatitis A Antigen Test Grouping Value Set"
//			codeSystem="2.16.840.1.113883.3.560.101.1" />
//		<title>Laboratory Test, Result: Hepatitis A Antigen Test (result:
//			'Seropositive')</title>
//		<statusCode code="completed" />
//		<sourceOf typeCode="REFR">
//			<observation classCode="OBS" moodCode="EVN"
//				isCriterionInd="true">
//				<templateId root="2.16.840.1.113883.3.560.1.1019.2" />
//				<code code="385676005" codeSystem="2.16.840.1.113883.6.96"
//					displayName="result" codeSystemName="SNOMED-CT" />
//				<value xsi:type="CD" code="2.16.840.1.113883.3.464.1003.110.12.1054"
//					codeSystem="2.16.840.1.113883.3.560.101.1" displayName="result" />
//			</observation>
//		</sourceOf>
//	</observation>
//</sourceOf>
//</act>

public class ActObject {
	String classCode;
	String moodCode;
	boolean isCriterianInd;
}
