package me.osm.gazetter.join.out_handlers;

import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

public class PrintJoinOutHandler extends SingleWriterJOHBase {
	
	public static final String NAME = "out-print";

	@Override
	public void handle(JSONObject object, String stripe) {
		println(object.toString());
	}


}
