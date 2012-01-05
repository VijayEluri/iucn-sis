package org.iucn.sis.shared.api.models.primitivefields;

import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.PrimitiveField;

/**
 * BooleanUnknownPrimitiveField generated by hbm2java
 */
public class BooleanUnknownPrimitiveField extends PrimitiveField<Integer> implements
		java.io.Serializable {
	
	public static final Integer YES = 1;
	public static final Integer NO = 2;
	public static final Integer UNKNOWN = 3;

	private Integer value;

	public BooleanUnknownPrimitiveField() {
	}

	public BooleanUnknownPrimitiveField(String name, Field field) {
		super(name, field);
	}
	
	public BooleanUnknownPrimitiveField(String name, Field field, Integer value) {
		super(name, field);
		this.value = value;
	}

	public Integer getValue() {
		return this.value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	@Override
	public String getSimpleName() {
		return PrimitiveFieldType.BOOLEAN_UNKNOWN_PRIMITIVE.getName();
	}
	
	@Override
	public BooleanUnknownPrimitiveField deepCopy() {
		BooleanUnknownPrimitiveField ret = new BooleanUnknownPrimitiveField();
		copyInto(ret);
		return ret;
	}

	@Override
	public void setRawValue(String value) {
		try {
			setValue(Integer.parseInt(value.trim()));
		} catch (NumberFormatException e) {
			if( Boolean.parseBoolean(value.trim()) ) {
				setValue(1);
			} else if( !value.trim().equalsIgnoreCase("unknown")) {
				setValue(2);
			} else {
				setValue(3);
			}
		}
	}
	
	public static String getDisplayString(Integer data) {
		return getDisplayString(data, null);
	}
	
	public static String getDisplayString(Integer data, String defaultValue) {
		String result;
		if (UNKNOWN.equals(data))
			result = "Unknown";
		else if (YES.equals(data))
			result = "Yes";
		else if (NO.equals(data))
			result = "No";
		else
			result = defaultValue;	
		
		return result;
	}
}
