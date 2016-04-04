package com.emc.documentum.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.emc.d2fs.models.attribute.Attribute;
import com.emc.d2fs.models.item.Item;
import com.emc.d2fs.models.node.Node;
import com.emc.documentum.dtos.DocumentumCabinet;
import com.emc.documentum.dtos.DocumentumFolder;
import com.emc.documentum.dtos.DocumentumObject;

public class DCD2Transformation {
	private DCD2Transformation(){
		
	}
	public static <T extends DocumentumObject> T convertD2NodeObject(Node d2Object)
	{
		T object = null ;
			object = (T) createDocumentumObject(d2Object.getType());
			object.setId(d2Object.getId());
			object.setName(d2Object.getLabel());
			object.setType(d2Object.getType());
			if(d2Object.getAttributes()!=null)
			{
				object.setProperties(convertD2Properties(d2Object.getAttributes()));
			}
		return object;
	}
	public static <T extends DocumentumObject> T convertD2DocItemObject(Item d2Object)
	{
		T object = null ;
			object = (T) createDocumentumObject(d2Object.getType());
			object.setId(d2Object.getId());
			object.setType(d2Object.getType());
			if(d2Object.getAttributes()!=null)
			{
				object.setProperties(convertD2Properties(d2Object.getAttributes()));
				object.setName((String) object.getProperties().get("object_name"));
				object.getProperties().remove("object_name");
			}
		return object;
	}
	@SuppressWarnings("unchecked")
	public static <T extends DocumentumObject> ArrayList<T> convertD2DocItemObjectList(List<Item> list) {
		ArrayList<T> documentumObject = new ArrayList<T>();
		for (Item d2object : list) {
			documentumObject.add((T) convertD2DocItemObject(d2object));
		}
		return documentumObject;
	}
	
	private static DocumentumObject createDocumentumObject(String baseTypeId) {
		DocumentumObject documentumObject;
		switch (baseTypeId) {
		case "dm_folder":
			documentumObject = new DocumentumFolder();
			break;
		case "dm_cabinet":
			documentumObject = new DocumentumCabinet();
			break;
		case "dm_document":

		default:
			documentumObject = new DocumentumObject();
		}
		return documentumObject;
	}
	

	@SuppressWarnings("unchecked")
	public static <T extends DocumentumObject> ArrayList<T> convertD2NodeObjectList(List<Node> list) {
		ArrayList<T> documentumObject = new ArrayList<T>();
		for (Node d2object : list) {
			documentumObject.add((T) convertD2NodeObject(d2object));
		}
		return documentumObject;
	}
	public static HashMap<String,Object> convertD2Properties(List<Attribute> attributes)
	{
		HashMap<String, Object> properties = new HashMap<String, Object>();
		for(Attribute attribute:attributes)
		{
			properties.put(attribute.getName(), attribute.getValue());
		}
		return properties;
	}

}
