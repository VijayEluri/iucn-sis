package org.iucn.sis.shared.api.views.components;

import java.util.ArrayList;


public class ContentRule extends Rule {

	public ContentRule(String rule) {
		this(rule, new ArrayList<Integer>());
	}

	public ContentRule(String rule, ArrayList<Integer> affectedObjectIndices) {
		super(rule, affectedObjectIndices);
	}

	public boolean matchesContent(String content) {
		return rule.equalsIgnoreCase(content);
	}

	@Override
	public String toXML() {
		String xmlRetString = "<contentRule>\n";
		xmlRetString += "\t<activateOnContent>" + rule + "</activateOnContent>\n";
		xmlRetString += "\t<actions>\n";
		xmlRetString += "\t\t<onTrue>" + onTrue + "</onTrue>\n";
		xmlRetString += "\t\t<onFalse>" + onFalse + "</onFalse>\n";
		for (int i = 0; i < affectedObjectIndices.size(); i++) {
			xmlRetString += "\t\t<affectedDependentStructures>" + ((Integer) affectedObjectIndices.get(i)).intValue()
					+ "</affectedDependentStructures>\n";
		}
		xmlRetString += "\t</actions>\n";
		xmlRetString += "\t</contentRule>";
		return xmlRetString;
	}

}
