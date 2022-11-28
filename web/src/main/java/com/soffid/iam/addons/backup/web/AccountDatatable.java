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
import com.soffid.iam.api.System;
import com.soffid.iam.web.component.DatatypeColumnsDatatable;

import es.caib.seycon.ng.comu.AccountAccessLevelEnum;
import es.caib.seycon.ng.comu.TypeEnumeration;
import es.caib.zkib.datamodel.DataNode;

public class AccountDatatable extends DatatypeColumnsDatatable {
	static String[] defaultColumns = {
			"account.name", "account.description", "account.system", "start", "end", "level"
	};
	
	public AccountDatatable() throws Exception {
	}
	
	public Collection<DataType> getDataTypes() throws Exception {
		HashSet<String> names = new HashSet<>();
		LinkedList<DataType> l = new LinkedList<DataType>( 
				EJBLocator.getAdditionalDataService().findDataTypesByObjectTypeAndName2(Account.class.getName(), null));
		for (Iterator<DataType> it = l.iterator(); it.hasNext();) {
			DataType dt = it.next();
			if ( Boolean.TRUE.equals( dt.getBuiltin()))
				dt.setName("account."+dt.getName());
			else
				dt.setName("account.attributes."+dt.getName());
			dt.setBuiltin(true);
			names.add(dt.getName());
			if (dt.getType() == TypeEnumeration.SEPARATOR)
				it.remove();
		}
		
		for (System system: EJBLocator.getDispatcherService().findAllActiveDispatchers()) {
			for ( DataType dt: EJBLocator.getAdditionalDataService().findSystemDataTypes(system.getName())) {
				if ( Boolean.TRUE.equals( dt.getBuiltin()))
					dt.setName("account."+dt.getName());
				else
					dt.setName("account.attributes."+dt.getName());
				if (!names.contains(dt.getName())) {
					names.add(dt.getName());
					dt.setBuiltin(true);
					if (dt.getType() != TypeEnumeration.SEPARATOR)
						l.add(dt);
				}
			}
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
		
		dt = new DataType();
		dt.setLabel(Labels.getLabel("aplicaIntranet_autoritzacio.Level"));
		dt.setName("level");
		dt.setBuiltin(true);
		dt.setEnumeration(AccountAccessLevelEnum.class.getName());
		dt.setType(TypeEnumeration.STRING_TYPE);
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
		AccountHistory gu = (AccountHistory) ((DataNode)element).getInstance();
		if (gu.getEnd() != null && new Date().after(gu.getEnd()))
			s.put("$class", "dashed");
		return s;
	}

}
