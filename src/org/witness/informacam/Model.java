package org.witness.informacam;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.util.Log;

public class Model extends JSONObject {
	public final static String LOG = "*********************** ICTD Utility ***********************";
	Field[] fields;

	public void inflate(byte[] jsonStringBytes) {
		try {
			if(jsonStringBytes != null) {
				inflate((JSONObject) new JSONTokener(new String(jsonStringBytes)).nextValue());
			} else {
				Log.d(LOG, "json is null, no inflate");
			}
		} catch (JSONException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		} catch(NullPointerException e) {
			Log.e(LOG, e.toString());
			e.printStackTrace();
		}
	}

	public void inflate(Model model) {
		inflate(model.asJson());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void inflate(JSONObject values) {
		fields = this.getClass().getFields();
		//Log.d(LOG, "MODEL:\n" + values.toString());

		for(Field f : fields) {
			try {
				f.setAccessible(true);
				if(values.has(f.getName())) {
					boolean isModel = false;

					if(f.getType().getSuperclass() == Model.class) {
						isModel = true;
					}					

					if(f.getType() == List.class) {
						List subValue = new ArrayList();
						Class clz = (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];

						Object test = clz.newInstance();
						if(test instanceof Model) {
							isModel = true;
						}

						JSONArray ja = values.getJSONArray(f.getName());
						for(int i=0; i<ja.length(); i++) {
							Object value = clz.newInstance();
							if(isModel) {
								((Model) value).inflate(ja.getJSONObject(i));
							} else {
								value = ja.get(i);
							}
							subValue.add(value);
						}

						f.set(this, subValue);
					} else if(f.getType() == byte[].class) { 
						f.set(this, values.getString(f.getName()).getBytes());
					} else if(f.getType() == float[].class) {
						f.set(this, parseJSONAsFloatArray(values.getString(f.getName())));
					} else if(f.getType() == int[].class) {
						f.set(this, parseJSONAsIntArray(values.getString(f.getName())));
					} else if(isModel) {						
						Class clz = (Class<?>) f.getType();
						// if clz has less fields than the json object, this could be a subclass
						Object val = clz.newInstance();
						((Model) val).inflate(values.getJSONObject(f.getName()));
						f.set(this, val);
					} else {
						f.set(this, values.get(f.getName()));
					}
				}
			} catch (IllegalArgumentException e) {
				Log.e(LOG, e.toString());
				e.printStackTrace();
			} catch (JSONException e) {
				Log.e(LOG, e.toString());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.e(LOG, e.toString());
				e.printStackTrace();
			} catch (InstantiationException e) {
				Log.e(LOG, e.toString());
				e.printStackTrace();
			}
		}
	}
	
	public static int[] parseJSONAsIntArray(String value) {
		String[] intStrings = value.substring(1, value.length() - 1).split(",");
		int[] ints = new int[intStrings.length];

		for(int f=0; f<intStrings.length; f++) {
			ints[f] = Integer.parseInt(intStrings[f]);
		}

		return ints;
	}

	public static JSONArray parseIntArrayAsJSON(int[] ints) {
		JSONArray intArray = new JSONArray();
		for(int f : ints) {
			intArray.put(f);
		}

		return intArray;
	}

	public static float[] parseJSONAsFloatArray(String value) {
		String[] floatStrings = value.substring(1, value.length() - 1).split(",");
		float[] floats = new float[floatStrings.length];

		for(int f=0; f<floatStrings.length; f++) {
			floats[f] = Float.parseFloat(floatStrings[f]);
		}

		return floats;
	}

	public static JSONArray parseFloatArrayAsJSON(float[] floats) {
		JSONArray floatArray = new JSONArray();
		for(float f : floats) {
			try {
				floatArray.put(f);
			} catch (JSONException e) {
				Log.e(LOG, e.toString());
				e.printStackTrace();
			}
		}

		return floatArray;
	}
	
	public JSONObject asJson() {
		fields = this.getClass().getFields();
		JSONObject json = new JSONObject();

		for(Field f : fields) {
			f.setAccessible(true);

			try {
				Object value = f.get(this);

				if(f.getName().contains("this$")) {
					continue;
				}

				if(f.getName().equals("NULL") || f.getName().equals("LOG")) {
					continue;
				}

				boolean isModel = false;

				if(f.getType().getSuperclass() == Model.class) {
					isModel = true;
				}

				if(f.getType() == List.class) {
					JSONArray subValue = new JSONArray();
					for(Object v : (List<?>) value) {
						if(v instanceof Model) {
							subValue.put(((Model) v).asJson());
						} else {
							subValue.put(v);
						}
					}

					json.put(f.getName(), subValue);
				} else if(f.getType() == byte[].class) {
					json.put(f.getName(), new String((byte[]) value));
				} else if(f.getType() == float[].class) {
					json.put(f.getName(), parseFloatArrayAsJSON((float[]) value));
				} else if(f.getType() == int[].class) {
					json.put(f.getName(), parseIntArrayAsJSON((int[]) value));
				} else if(isModel) {
					json.put(f.getName(), ((Model) value).asJson());
				} else {
					json.put(f.getName(), value);
				}
			} catch (IllegalArgumentException e) {
				Log.d(LOG, e.toString());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d(LOG, e.toString());
				e.printStackTrace();
			} catch (JSONException e) {
				Log.d(LOG, e.toString());
				e.printStackTrace();
			} catch (NullPointerException e) {

			}

		}

		return json;
	}

}