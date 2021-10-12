package com.soffid.iam.addons.backup.web;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.util.resource.Labels;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.GroupUser;
import com.soffid.iam.web.component.DatatypeColumnsDatatable;

import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.zkib.datamodel.DataNode;

public class UserGroupDatatable extends DatatypeColumnsDatatable {
	static String[] defaultColumns = {
			"user", "fullName", "group", "groupDescription", "start", "end"
	};
	
	public UserGroupDatatable() throws Exception {
	}
	
	public Collection<DataType> getDataTypes() throws Exception {
		LinkedList<DataType> l = new LinkedList<DataType>( 
				EJBLocator.getAdditionalDataService().findDataTypesByObjectTypeAndName2(GroupUser.class.getName(), null));
		for (Iterator<DataType> it = l.iterator(); it.hasNext();) {
			DataType dt = it.next();
			if (dt.getType() == TypeEnumeration.SEPARATOR)
				it.remove();
		}
		DataType dt = new DataType();
		dt.setLabel(Labels.getLabel("inbox.lblFecha"));
		dt.setName("start");
		dt.setBuiltin(true);
		dt.setType(TypeEnumeration.DATE_TIME_TYPE);
		l.add(dt);

		dt = new DataType();
		dt.setLabel(Labels.getLabel("llistaRegistreAccesUsuari.zul.Datafi"));
		dt.setName("end");
		dt.setBuiltin(true);
		dt.setType(TypeEnumeration.DATE_TIME_TYPE);
		l.add(dt);
		
		return l;
	}

	@Override
	public String[] getDefaultColumns() throws Exception {
		return defaultColumns;
	}

	@Override
	protected JSONObject getClientValue(Object element) throws JSONException {
		JSONObject s = super.getClientValue(element);
		GroupUser gu = (GroupUser) ((DataNode)element).getInstance();
		if (Boolean.TRUE.equals(gu.getDisabled()))
			s.put("$class", "dashed");
		return s;
	}

}
