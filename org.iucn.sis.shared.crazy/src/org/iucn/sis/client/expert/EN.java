package org.iucn.sis.client.expert;

import java.util.HashMap;

import com.solertium.lwxml.gwt.debug.SysDebugger;

/**
 * Represents the endangered class
 * 
 * @author liz.schwartz
 * 
 */
class EN {

	// NUMBER OF CRITERIA FOR EACH LETTER
	public final int criteriaA = 4;
	public final int criteriaB = 2;
	public final int criteriaC = 2;
	public final int criteriaD = 1;
	public final int criteriaE = 1;

	// RANGES FOR EACH CRITERIA
	public Range a1;
	public Range a2;
	public Range a3;
	public Range a4;
	public Range b1;
	public Range b2;
	public Range c1;
	public Range c2;
	public Range d;
	public Range e;

	// FACTORS IN EACH CRITERIA
	public final String[] factorsA1 = new String[] { Factors.populationReductionPast,
			Factors.populationReductionPastReversible, Factors.populationReductionPastUnderstood,
			Factors.populationReductionPastCeased };
	public final String[] factorsA2 = new String[] { Factors.populationReductionPast,
			Factors.populationReductionPastReversible, Factors.populationReductionPastUnderstood,
			Factors.populationReductionPastCeased };
	public final String[] factorsA3 = new String[] { Factors.populationReductionFuture };
	public final String[] factorsA4 = new String[] { Factors.populationReductionEither,
			Factors.populationReductionEitherCeased, Factors.populationReductionEitherUnderstood,
			Factors.populationReductionEitherReversible };
	public final String[] factorsB1 = new String[] { Factors.extent, Factors.severeFragmentation, Factors.locations,
			Factors.extentDecline, Factors.areaDecline, Factors.habitatDecline, Factors.locationDecline,
			Factors.subpopulationDecline, Factors.populationDecline, Factors.extentFluctuation,
			Factors.areaFluctuation, Factors.locationFluctuation, Factors.subpopulationFluctuation,
			Factors.populationFluctuation };
	public final String[] factorsB2 = new String[] { Factors.area, Factors.severeFragmentation, Factors.locations,
			Factors.extentDecline, Factors.areaDecline, Factors.habitatDecline, Factors.locationDecline,
			Factors.subpopulationDecline, Factors.populationDecline, Factors.extentFluctuation,
			Factors.areaFluctuation, Factors.locationFluctuation, Factors.subpopulationFluctuation,
			Factors.populationFluctuation };
	public final String[] factorsC1 = new String[] { Factors.populationSize, Factors.populationDeclineGenerations2 };
	public final String[] factorsC2 = new String[] { Factors.populationSize, Factors.populationDecline,
			Factors.subpopulationSize, Factors.populationFluctuation };
	public final String[] factorsD = new String[] { Factors.populationSize };
	public final String[] factorsE = new String[] { Factors.extinctionGenerations5 };

	// A1, A2, or A3 must be true in order for A to be true
	private final int aPopulationReductionPast1 = 70; // >=70
	private final int aPopulationReductionPast2 = 50; // >=
	private final int aPopulationReductionFuture = 50; // >=
	private final int aPopulationReductionEither = 50; // >=

	// B1 or B2 has to be true for B to be true
	private final int bExtent = 5000; // extent < 5000 km^2
	private final int bArea = 500; // area < 500 km^2
	private final int bLocations = 5; // a locations<=5

	// C -- populationSize and (C1 or C2)
	private final int cPopulationSize = 2500; // <250
	private final int cPopulationDeclineGenerations2 = 25; // >=25
	private final int cMaxSubpopulationSize = 250; // <=250
	private final double cAlotInSubpopulation = 0.95; // (maxSubpopulationSize)/
	// populationSize >= .95

	// D
	private final int dPopulationSize = 250; // < 250

	// E
	private final int eExtinctionGenerations5 = 20; // >=20

	public EN() {

	}

	public CriteriaResult a1(HashMap factors, String populationReductionBasis) {
		CriteriaResult analysis = new CriteriaResult();
		Range result = null;
		String[] csv = populationReductionBasis.split(",");

		if (!(csv.length == 1 && csv[0] == "0")) {
			Range ppr = Range.greaterthanequal((Range) factors.get(Factors.populationReductionPast),
					(float) aPopulationReductionPast1);
			Range prpr = (Range) factors.get(Factors.populationReductionPastReversible);
			Range prpu = (Range) factors.get(Factors.populationReductionPastUnderstood);
			Range prpc = (Range) factors.get(Factors.populationReductionPastCeased);

			result = Range.independentAND(ppr, prpr);
			result = Range.independentAND(result, prpu);
			result = Range.independentAND(result, prpc);
		}
		a1 = result;
		analysis.range = result;
		analysis.resultString = createAString(result, csv, "1");
		if (analysis.resultString.equalsIgnoreCase(""))
			analysis.range = null;
		
		return analysis;
	}

	public CriteriaResult a2(HashMap factors, String populationReductionBasis) {
		CriteriaResult analysis = new CriteriaResult();
		Range result = null;
		String[] csv = populationReductionBasis.split(",");

		if (!(csv.length == 1 && csv[0] == "0")) {
			Range ppr = Range.greaterthanequal((Range) factors.get(Factors.populationReductionPast),
					(float) aPopulationReductionPast2);
			Range prpr = (Range) factors.get(Factors.populationReductionPastReversible);
			Range prpu = (Range) factors.get(Factors.populationReductionPastUnderstood);
			Range prpc = (Range) factors.get(Factors.populationReductionPastCeased);

			prpc = Range.isFalse(prpc);
			prpu = Range.isFalse(prpu);
			prpr = Range.isFalse(prpr);

			result = Range.independentOR(prpc, prpu);
			result = Range.independentOR(result, prpr);
			result = Range.independentAND(result, ppr);

		}
		a2 = result;
		analysis.range = result;
		analysis.resultString = createAString(result, csv, "2");
		if (analysis.resultString.equalsIgnoreCase(""))
			analysis.range = null;
		
		return analysis;

	}

	public CriteriaResult a3(Range prf, String populationReductionFutureBasis) {
		Range result = null;
		CriteriaResult analysis = new CriteriaResult();
		String[] csv = populationReductionFutureBasis.split(",");

		if (!(csv.length == 1 && csv[0] == "0")) {
			prf = Range.greaterthanequal(prf, aPopulationReductionFuture);
			result = prf;
		}
		a3 = result;
		analysis.range = result;
		analysis.resultString = createA3String(result, csv);
		if (analysis.resultString.equalsIgnoreCase(""))
			analysis.range = null;
		
		return analysis;
	}

	public CriteriaResult a4(HashMap factors, String populationReductionEitherBasis) {
		Range result = null;
		CriteriaResult analysis = new CriteriaResult();
		String[] csv = populationReductionEitherBasis.split(",");

		if (!(csv.length == 1 && csv[0] == "0")) {
			Range prpb = new Range("1");
			Range pre = (Range) factors.get(Factors.populationReductionEither);
			pre = Range.greaterthanequal(pre, aPopulationReductionEither);
			Range prec = (Range) factors.get(Factors.populationReductionEitherCeased);
			Range preu = (Range) factors.get(Factors.populationReductionEitherUnderstood);
			Range prer = (Range) factors.get(Factors.populationReductionEitherReversible);
			prec = Range.isFalse(prec);
			preu = Range.isFalse(preu);
			prer = Range.isFalse(prer);
			result = Range.independentOR(prec, preu);
			result = Range.independentOR(result, prer);
			result = Range.independentAND(result, prpb);
			result = Range.independentAND(result, pre);

		}
		a4 = result;
		analysis.range = result;
		analysis.resultString = createAString(result, csv, "4");
		if (analysis.resultString.equalsIgnoreCase(""))
			analysis.range = null;
		
		return analysis;
	}

	public CriteriaResult b1(HashMap factors) {
		Range result = null;
		CriteriaResult analysis = new CriteriaResult();
		analysis.resultString = "";

		Range extent = (Range) factors.get(Factors.extent);
		Range and1 = Range.lessthan(extent, bExtent);

		// EXTENT < 5000
		if (and1 != null && !Range.isConstant(and1, 0)) {

			Range sf = (Range) factors.get(Factors.severeFragmentation);
			Range loc = (Range) factors.get(Factors.locations);
			loc = Range.lessthanequal(loc, bLocations);
			Range or1 = Range.independentOR(sf, loc);

			Range ed = (Range) factors.get(Factors.extentDecline);
			Range ad = (Range) factors.get(Factors.areaDecline);
			Range hd = (Range) factors.get(Factors.habitatDecline);
			Range ld = (Range) factors.get(Factors.locationDecline);
			Range sd = (Range) factors.get(Factors.subpopulationDecline);
			Range pd = (Range) factors.get(Factors.populationDecline);
			Range or2 = Range.independentOR(ed, ad);
			or2 = Range.independentOR(or2, hd);
			or2 = Range.independentOR(or2, ld);
			or2 = Range.independentOR(or2, sd);
			or2 = Range.independentOR(or2, pd);

			Range ef = (Range) factors.get(Factors.extentFluctuation);
			Range af = (Range) factors.get(Factors.areaFluctuation);
			Range lf = (Range) factors.get(Factors.locationFluctuation);
			Range sef = (Range) factors.get(Factors.subpopulationFluctuation);
			Range pf = (Range) factors.get(Factors.populationFluctuation);
			Range or3 = Range.independentOR(ef, af);
			or3 = Range.independentOR(or3, lf);
			or3 = Range.independentOR(or3, sef);
			or3 = Range.independentOR(or3, pf);

			if ((or1 != null && !Range.isConstant(or1, 0)) && (or2 != null && !Range.isConstant(or2, 0))
					&& (or3 != null && !Range.isConstant(or3, 0))) {

				Range and = Range.independentAND(and1, or1);
				and = Range.independentAND(and, or2);
				and = Range.independentAND(and, or3);
				b1 = and;
				analysis.range = and;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;

			} else if (or1 != null && !Range.isConstant(or1, 0)) {

				if (or2 != null && !Range.isConstant(or2, 0)) {
					Range and = Range.independentAND(and1, or1);
					and = Range.independentAND(and, or2);
					b1 = and;
					analysis.range = and;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);

					return analysis;
				} else if (or3 != null && !Range.isConstant(or3, 0)) {
					Range and = Range.independentAND(and1, or1);
					and = Range.independentAND(and, or3);
					b1 = and;
					analysis.range = and;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
					return analysis;
				}

				// NOT ENOUGH DATA
				else {
					b1 = result;
					analysis.range = result;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
					return analysis;
				}
			}

			else if ((or2 != null && !Range.isConstant(or2, 0)) && (or3 != null && !Range.isConstant(or3, 0))) {
				Range and = Range.independentAND(and1, or2);
				and = Range.independentAND(and, or3);
				b1 = and;
				analysis.range = and;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;
			}

			// NOT ENOUGH DATA
			else {
				b1 = result;
				analysis.range = result;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("1", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;
			}

		}
		// EXTENT >= 5000
		else {
			b1 = result;
			analysis.range = result;
			return analysis;
		}

	}

	public CriteriaResult b2(HashMap factors) {
		Range result = null;
		CriteriaResult analysis = new CriteriaResult();
		analysis.resultString = "";

		Range area = (Range) factors.get(Factors.area);
		Range and1 = Range.lessthan(area, bArea);

		// EXTENT < 100
		if (and1 != null && !Range.isConstant(and1, 0)) {

			Range sf = (Range) factors.get(Factors.severeFragmentation);
			Range loc = (Range) factors.get(Factors.locations);
			loc = Range.lessthanequal(loc, bLocations);
			Range or1 = Range.independentOR(sf, loc);

			Range ed = (Range) factors.get(Factors.extentDecline);
			Range ad = (Range) factors.get(Factors.areaDecline);
			Range hd = (Range) factors.get(Factors.habitatDecline);
			Range ld = (Range) factors.get(Factors.locationDecline);
			Range sd = (Range) factors.get(Factors.subpopulationDecline);
			Range pd = (Range) factors.get(Factors.populationDecline);
			Range or2 = Range.independentOR(ed, ad);
			or2 = Range.independentOR(or2, hd);
			or2 = Range.independentOR(or2, ld);
			or2 = Range.independentOR(or2, sd);
			or2 = Range.independentOR(or2, pd);

			Range ef = (Range) factors.get(Factors.extentFluctuation);
			Range af = (Range) factors.get(Factors.areaFluctuation);
			Range lf = (Range) factors.get(Factors.locationFluctuation);
			Range sef = (Range) factors.get(Factors.subpopulationFluctuation);
			Range pf = (Range) factors.get(Factors.populationFluctuation);
			Range or3 = Range.independentOR(ef, af);
			or3 = Range.independentOR(or3, lf);
			or3 = Range.independentOR(or3, sef);
			or3 = Range.independentOR(or3, pf);

			if ((or1 != null && !Range.isConstant(or1, 0)) && (or2 != null && !Range.isConstant(or2, 0))
					&& (or3 != null && !Range.isConstant(or3, 0))) {

				Range and = Range.independentAND(and1, or1);
				and = Range.independentAND(and, or2);
				and = Range.independentAND(and, or3);
				b2 = and;
				analysis.range = and;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;

			} else if (or1 != null && !Range.isConstant(or1, 0)) {

				if (or2 != null && !Range.isConstant(or2, 0)) {
					Range and = Range.independentAND(and1, or1);
					and = Range.independentAND(and, or2);
					b2 = and;
					analysis.range = and;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
					return analysis;
				} else if (or3 != null && !Range.isConstant(or3, 0)) {
					Range and = Range.independentAND(and1, or1);
					and = Range.independentAND(and, or3);
					b2 = and;
					analysis.range = and;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
					return analysis;
				}

				// NOT ENOUGH DATA
				else {
					b2 = result;
					analysis.range = result;
					if (analysis.range != null && !Range.isConstant(analysis.range, 0))
						analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
					return analysis;
				}
			}

			else if ((or2 != null && !Range.isConstant(or2, 0)) && (or3 != null && !Range.isConstant(or3, 0))) {
				Range and = Range.independentAND(and1, or2);
				and = Range.independentAND(and, or3);
				b2 = and;
				analysis.range = and;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;
			}

			// NOT ENOUGH DATA
			else {
				b2 = result;
				analysis.range = result;
				if (analysis.range != null && !Range.isConstant(analysis.range, 0))
					analysis.resultString = createBString("2", sf, ed, ad, hd, ld, sd, pd, ef, af, lf, sef, pf);
				return analysis;
			}

		}
		// EXTENT >= 5000
		else {
			b2 = result;
			analysis.range = result;
			return analysis;
		}

	}

	public CriteriaResult c1(HashMap factors) {
		CriteriaResult analysis = new CriteriaResult();

		Range ps = (Range) factors.get(Factors.populationSize);
		Range pdg2 = (Range) factors.get(Factors.populationDeclineGenerations2);
		Range ps1 = Range.lessthan(ps, cPopulationSize);
		pdg2 = Range.greaterthanequal(pdg2, cPopulationDeclineGenerations2);
		Range and1 = Range.independentAND(ps1, pdg2);

		c1 = and1;
		analysis.range = and1;
		if ((c1 != null) && (!Range.isConstant(c1, 0)))
			analysis.resultString = "C1";
		return analysis;

	}

	public CriteriaResult c2(HashMap factors) {
		CriteriaResult analysis = new CriteriaResult();

		Range ps = (Range) factors.get(Factors.populationSize);
		Range ps1 = Range.lessthan(ps, cPopulationSize);
		Range pd = (Range) factors.get(Factors.populationDecline);

		Range sps = (Range) factors.get(Factors.subpopulationSize);
		Range div = Range.divide(sps, ps);
		div = Range.greaterthanequal(div, (float) cAlotInSubpopulation);
		sps = Range.lessthanequal(sps, cMaxSubpopulationSize);

		Range pf = (Range) factors.get(Factors.populationFluctuation);

		Range or1 = Range.independentOR(sps, div);
		or1 = Range.independentOR(or1, pf);
		Range and2 = Range.independentAND(or1, pd);
		Range result = Range.independentAND(ps1, and2);

		c2 = result;
		analysis.range = result;
		if ((c2 != null) && (!Range.isConstant(c2, 0)))
			analysis.resultString = createC2String(sps, div, pf);
		else
			analysis.resultString = "";
		return analysis;

	}

	private String createA3String(Range result, String[] csv) {
		boolean stringSet = false;
		String resultString = "";
		if ((result != null) && (!(Range.isConstant(result, 0)))) {
			for (int i = 0; i < csv.length; i++) {
				if (csv[i].equals("1")) {
					if (stringSet)
						resultString += "b";
					else
						resultString = "A" + 3 + "b";
				} else if (csv[i].equals("2")) {
					if (stringSet)
						resultString += "c";
					else
						resultString = "A" + 3 + "c";
					stringSet = true;
				} else if (csv[i].equals("3")) {
					if (stringSet)
						resultString += "d";
					else
						resultString = "A" + 3 + "d";
					stringSet = true;
				} else if (csv[i].equals("4")) {
					if (stringSet)
						resultString += "e";
					else
						resultString = "A" + 3 + "e";
					stringSet = true;
				}
			}
		}

		return resultString;
	}

	private String createAString(Range result, String[] csv, String number) {
		boolean stringSet = false;
		String resultString = "";
		if ((result != null) && (!(Range.isConstant(result, 0)))) {
			for (int i = 0; i < csv.length; i++) {
				if (csv[i].equals("1")) {
					if (stringSet)
						resultString += "a";
					else
						resultString = "A" + number + "a";
					stringSet = true;
				} else if (csv[i].equals("2")) {
					if (stringSet)
						resultString += "b";
					else
						resultString = "A" + number + "b";
					stringSet = true;
				} else if (csv[i].equals("3")) {
					if (stringSet)
						resultString += "c";
					else
						resultString = "A" + number + "c";
					stringSet = true;
				} else if (csv[i].equals("4")) {
					if (stringSet)
						resultString += "d";
					else
						resultString = "A" + number + "d";
					stringSet = true;
				} else if (csv[i].equals("5")) {
					if (stringSet)
						resultString += "e";
					else
						resultString = "A" + number + "e";
					stringSet = true;
				}
			}
		}

		return resultString;
	}

	private String createBString(String number, Range sf, Range ed, Range ad, Range hd, Range ld, Range sd, Range pd,
			Range ef, Range af, Range lf, Range sef, Range pf) {
		String returnString = "";
		String aString = "";
		String bString = "";
		String cString = "";
		boolean stringStarted = false;
		boolean bStarted = false;
		boolean cStarted = false;

		// DO C STRING
		if ((ef != null) && (!(Range.isConstant(ef, 0)))) {
			if (cStarted) {
				cString += ",i";
			} else
				cString += "i";
			cStarted = true;
		}
		if ((af != null) && (!(Range.isConstant(af, 0)))) {
			if (cStarted) {
				cString += ",ii";
			} else
				cString += "ii";
			cStarted = true;
		}
		if ((lf != null) && (!(Range.isConstant(lf, 0))) || ((sef != null) && (!(Range.isConstant(sef, 0))))) {
			if (cStarted) {
				cString += ",iii";
			} else
				cString += "iii";
			cStarted = true;
		}
		if ((pf != null) && (!(Range.isConstant(pf, 0)))) {
			if (cStarted) {
				cString += ",iv";
			} else
				cString += "iv";
			cStarted = true;
		}
		if (cStarted) {
			cString = "c(" + cString + ")";
		}

		// DO B STRING
		if ((ed != null) && (!(Range.isConstant(ed, 0)))) {
			if (bStarted) {
				bString += ",i";
			} else
				bString += "i";
			bStarted = true;
		}
		if ((ad != null) && (!(Range.isConstant(ad, 0)))) {
			if (bStarted) {
				bString += ",ii";
			} else
				bString += "ii";
			bStarted = true;
		}
		if ((hd != null) && (!(Range.isConstant(hd, 0)))) {
			if (bStarted) {
				bString += ",iii";
			} else
				bString += "iii";
			bStarted = true;
		}
		if ((ld != null) && (!(Range.isConstant(ld, 0))) || ((sd != null) && (!(Range.isConstant(sd, 0))))) {
			if (bStarted) {
				bString += ",iv";
			} else
				bString += "iv";
			bStarted = true;
		}
		if ((pd != null) && (!(Range.isConstant(pd, 0)))) {
			if (bStarted) {
				bString += ",v";
			} else
				bString += "v";
			bStarted = true;
		}
		if (bStarted) {
			bString = "b(" + bString + ")";
		}

		// DO A STRING
		if ((sf != null) || (!(Range.isConstant(sf, 0)))) {
			aString = "a";
			stringStarted = true;
		}

		if (stringStarted || bStarted || cStarted) {
			returnString = "B" + number + aString + bString + cString;
		}

		return returnString;
	}

	private String createC2String(Range sps, Range div, Range pf) {
		String c2String = "";
		String aString = "";
		String bString = "";

		if ((sps != null) && (!(Range.isConstant(sps, 0)))) {
			aString = "i";
		}
		if ((div != null) && (!(Range.isConstant(div, 0)))) {
			if (aString.equals(""))
				aString = "ii";
			else
				aString += ",ii";
		}
		if (!aString.equals(""))
			aString = "a(" + aString + ")";

		if ((pf != null) && (!(Range.isConstant(pf, 0)))) {
			bString = "b";
		}

		c2String = aString + bString;
		if (!c2String.equals(""))
			c2String = "C2" + c2String;

		return c2String;
	}

	public CriteriaResult d(Range ps) {
		CriteriaResult analysis = new CriteriaResult();
		d = Range.lessthan(ps, dPopulationSize);
		analysis.range = d;
		if ((d != null) && (!(Range.isConstant(d, 0)))) {
			analysis.resultString = "D";
		}
		return analysis;
	}

	public CriteriaResult e(Range eg5) {
		CriteriaResult analysis = new CriteriaResult();
		e = Range.greaterthanequal(eg5, eExtinctionGenerations5);
		analysis.range = e;
		if ((e != null) && (!(Range.isConstant(e, 0)))) {
			analysis.resultString = "E";
		}
		return analysis;
	}

	private void printRange(String descrip, Range range) {
		if (range != null) {
			SysDebugger.getInstance().println(
					"This is the results from " + descrip + " " + range.getLow() + "," + range.getLowBest() + ","
							+ range.getHighBest() + "," + range.getHigh());
		} else {
			SysDebugger.getInstance().println(" " + descrip + " == null");

		}
	}

}
