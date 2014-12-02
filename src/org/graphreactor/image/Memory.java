package org.graphreactor.image;

import java.util.LinkedHashMap;

public class Memory {

	private LinkedHashMap<String, Object> column = new LinkedHashMap();

	public void setProperty(String key, Object value) {
		
		column.put(key, value);
		
	} 
	
}
