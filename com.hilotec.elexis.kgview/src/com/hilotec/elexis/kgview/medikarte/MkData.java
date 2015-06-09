package com.hilotec.elexis.kgview.medikarte;

import ch.elexis.data.Anwender;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class MkData extends PersistentObject {
	public static final String VERSION = "1";
	public static final String PLUGIN_ID = "com.hilotec.elexis.kgview";
	
	private static final String TABLENAME = "COM_HILOTEC_ELEXIS_KGVIEW_MKDATA";
	public static final String PATIENT_ID = "PatientID";
	public static final String LAST_EDITED = "LastEdited";
	public static final String FLD_AUTOR = "Autor";
	public static final String INFO = "Info";

	private static org.slf4j.Logger log=LoggerFactory.getLogger(MkData.class);

	static {
		addMapping(TABLENAME, "PatientID", "LastEdited", "Autor", "Info");
		checkTable();
	}
	
	private static final String create = "CREATE TABLE " + TABLENAME + " ("
		+ "  ID				VARCHAR(25) PRIMARY KEY, " 
		+ "  lastupdate 	BIGINT, "
		+ "  deleted		CHAR(1) DEFAULT '0', " 
		+ "  PatientID		VARCHAR(25), " 
		+ "  LastEdited 	TEXT, "
	    + "  Autor          VARCHAR(25),  " 
		+ "  Info			TEXT); "
	    + "	 INSERT INTO " + TABLENAME + " (ID, Info, PatientID) VALUES ('VERSION', '"+ VERSION +"', 'VERSION');";
	
	private static void checkTable(){
		
		String fm = null;
		try {
			fm =
				getConnection().queryString(
					"SELECT Info FROM " + TABLENAME + " WHERE ID='VERSION';");
		} catch (Exception e) {}
		
		if (fm == null) {
			createOrModifyTable(create);
		}
	}
	
	
	public MkData(Patient p, String info){
		String id =	getConnection().queryString(
					"SELECT ID FROM " + TABLENAME + " WHERE PatientID='" + p.getId() + "';"
					);
		if (id != null){
			MkData md = new MkData(id);
			md.setLastEdited(new TimeTool().toString(TimeTool.DATE_GER));
		}else{
			log.debug("no id" +id);
			create(null);
			try{
				set(new String[] {
					PATIENT_ID, LAST_EDITED, INFO, 
				}, p.getId(), new TimeTool().toString(TimeTool.DATE_GER), info);
			}catch(Exception e){}
		}
	}
	
	public static MkData load(final String id){
		String fm =
			getConnection().queryString(
				"SELECT PatientId FROM " + TABLENAME + " WHERE ID='" + id + "';");
		
		if (fm == null)
			return null;
		return new MkData(id);
	}
	
	public static MkData load(Patient p){
		if (p == null) {
			return null;
		}
		String id =	getConnection().queryString(
				"SELECT ID FROM " + TABLENAME + " WHERE PatientID='" + p.getId() + "';"
				);
		if (id != null){
			return new MkData(id);
		}else{
			// TODO: Koennte man mit einer Query sauberer loesen
			java.util.List<Prescription> medis = MedikarteHelpers.medikarteMedikation(p, false);
			TimeTool max = new TimeTool(0);
			TimeTool cur = new TimeTool();
			for (Prescription p1: medis) {
				cur.set(p1.getBeginDate());
				if (cur.isAfter(max)) max.set(p1.getBeginDate());
				cur.set(p1.getEndDate());
				if (cur.isAfter(max)) max.set(p1.getEndDate());
			}
			MkData md = new MkData(p, "");
			md.setLastEdited(max.toString(TimeTool.DATE_GER));
			return MkData.load(p);

		}
	}

	
	protected MkData(){}
	
	protected MkData(final String id){
		super(id);
	}
	

	
	@Override
	public String getLabel(){
		return null;
	}
	
	@Override
	protected String getTableName(){
		return TABLENAME;
	}
	
	public String getLastEdited(){
		if(exists()){
			return get(LAST_EDITED);
		}
		return null;
	}
	
	public Anwender getAutor(){
		String id = get(FLD_AUTOR);
		if (!StringTool.isNothing(id)) {
			Anwender aw = Anwender.load(id);
			return aw;
		}
		return null;
	}
	
	public void setLastEdited(String txt){
		set(LAST_EDITED, txt);
	}
	
	public void setAutor(Anwender anw){
		String id = "";
		if (anw != null)
			id = anw.getId();
		set(FLD_AUTOR, id);
	}
	
	
	/**
	 * Wir ueberschreiben hier set() um sicherzustellen dass nur der Autor einen KG-Eintrag anpassen
	 * kann, und um den Autor festzuhalten falls das noch nicht geschehen ist.
	 */
	@Override
	public boolean set(final String field, String value){

		return super.set(field, value);
	}
	
	@Override
	public boolean isDragOK(){
		return true;
	}
}

