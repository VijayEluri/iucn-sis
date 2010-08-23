package org.iucn.sis.shared.api.models.primitivefields;

import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.PrimitiveField;

/**
 * BooleanUnknownPrimitiveField generated by hbm2java
 */
public class BooleanUnknownPrimitiveField extends PrimitiveField<Integer> implements
		java.io.Serializable {

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
		return PrimitiveFieldFactory.BOOLEAN_UNKNOWN_PRIMITIVE;
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
}
