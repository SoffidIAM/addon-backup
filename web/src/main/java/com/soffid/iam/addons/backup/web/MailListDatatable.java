package com.soffid.iam.addons.backup.web;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.util.resource.Labels;

import com.soffid.iam.EJBLocator;
import com.soffid.iam.api.Account;
import com.soffid.iam.api.AccountHistory;
import com.soffid.iam.api.DataType;
import com.soffid.iam.api.GroupUser;
import com.soffid.iam.api.MailList;
import com.soffid.iam.api.System;
import com.soffid.iam.api.UserMailList;
import com.soffid.iam.web.component.DatatypeColumnsDatatable;

import es.caib.seycon.ng.comu.AccountAccessLevelEnum;
import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.zkib.datamodel.DataNode;

public class MailListDatatable extends DatatypeColumnsDatatable {
	static String[] defaultColumns = {
			"mailListName", "domainCode", "mailListDescription", "start", "end"
	};
	
	public MailListDatatable() throws Exception {
	}
	
	public Collection<DataType> getDataTypes() throws Exception {
		HashSet<String> names = new HashSet<>();
		Collection<DataType> l = new LinkedList<>();

		DataType dt = new DataType();
		dt.setName("mailListName");
		dt.setLabel(Labels.getLabel("com.soffid.iam.api.User.shortName"));
		dt.setBuiltin(true);
		dt.setType(TypeEnumeration.STRING_TYPE);
		l.add(dt);

		dt = new DataType();
		dt.setName("domainCode");
		dt.setLabel(Labels.getLabel("auditoria.zul.Dominicorreu"));
		dt.setBuiltin(true);
		dt.setType(TypeEnumeration.STRING_TYPE);
		l.add(dt);

		dt = new DataType();
		dt.setLabel(Labels.getLabel("autoritzacions.zul.Description"));
		dt.setName("mailListDescription");
		dt.setBuiltin(true);
		dt.setType(TypeEnumeration.STRING_TYPE);
		l.add(dt);

		dt = new DataType();
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
		UserMailList gu = (UserMailList) ((DataNode)element).getInstance();
		if (gu.getEnd() != null && new Date().after(gu.getEnd()))
			s.put("$class", "dashed");
		return s;
	}

}
